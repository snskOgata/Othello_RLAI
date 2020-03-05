import java.io.*;
import java.util.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class RLAI{

    //一々MainPanelに参照しにいくより幾分軽くなるかなと思って
	public static final int MASU = 8;
    // 空白
	private static final int BLANK = 0;
    // 黒石
	private static final int BLACK_STONE = 1;
    // 白石
	private static final int WHITE_STONE = -1;

	public int processedCount = 0;
	public int finCount = 0;
    //ε-greedy
	public double epsilon = 0.02;

    private MainPanel panel; //MainPanelと紐つける
    public ArrayList[] states; //状態とそれに対する行動とその確率を保持
    public ArrayList<StateAction> tmpSAs; //実際に行った状態・行動セット。1試合ごとにリセット
    public ArrayList<StateAction> doneSAs; //tmpSAsを用いて報酬も蓄えた状態・行動セット。1エピソード毎にリセット
    public ArrayList[] storedSAs; //doneSAsを整理したもの。これを用いて政策改善を行う→stateへ
    public int reuseCount; //statesから学習データを持ってきた回数（確認用）

    //実際の状態・行動を保持するためのクラス（これと報酬を用いて政策改善を行う）
    class StateAction {
    	public State state;
    	public Action doneAction;

    	public StateAction(State state, Action doneAction){
    		this.state = state;
    		this.doneAction = doneAction;
    	}
    }
    
    //@param panel メインパネルへの参照。
    public RLAI(){}
    public RLAI(MainPanel panel) {
    	this.panel = panel;
    	this.states = new ArrayList[64];
    	for(int i = 0; i < 64; i++){
    		this.states[i] = new ArrayList<State>();
    	}
    	this.doneSAs = new ArrayList<StateAction>();
    	this.storedSAs = new ArrayList[64];
    	for(int i = 0; i < 64; i++){
    		this.storedSAs[i] = new ArrayList<StateAction>();
    	}
    	this.tmpSAs = new ArrayList<StateAction>();
    	reuseCount = 0;
    }
    
    public void eternalForceBlizzard(){
    	//打てない時は敵のターン
    	if(panel.countCanPutDownStone() == 0){return;}

    	int x;
    	int y;
    	Action action;
    	State state;
	//状態を再利用または作成
    	state = this.reuseOrCreateState();

    	if(state.bestAction == null){
	    //政策に従って打つ手を決める
    		action = state.selectAction();
    		x = action.getX();
    		y = action.getY();
    	}else{
    		x = state.bestAction.getX();
    		y = state.bestAction.getY();
    	}
    	//打つべし打つべし
    	this.put(x, y);
	// AIがパスの場合はもう一回
    	if (panel.countCanPutDownStone() == 0) {
	    //	    	System.out.println("AI PASS!");
    		panel.nextTurn();
    		this.eternalForceBlizzard();
    	}

    }

    public void compute(){
	//打てない時は敵のターン
    	if(panel.countCanPutDownStone() == 0){return;}

    	int x;
    	int y;
    	Action action;
    	State state;
	//状態を再利用または作成
    	state = this.reuseOrCreateState();
	//政策に従って打つ手を決める
    	action = state.selectAction();
    	x = action.getX();
    	y = action.getY();
	//実際の状態と行動を記録
    	StateAction sa = new StateAction(state, action);
    	this.tmpSAs.add(sa);
	//対称となる状態・行動も保持
    	this.addOtherSAs(sa);

    	this.put(x, y);
	// AIがパスの場合はもう一回
    	if (panel.countCanPutDownStone() == 0) {
	    //	    	System.out.println("AI PASS!");
    		panel.nextTurn();
    		this.compute();
    	}
    }

    //状態を記憶済みか判断、それにより再利用か作成かを行う
    private State reuseOrCreateState(){
    	//this.statesに含まれるStateを走査
    	stateChange: for(State st : (ArrayList<State>)this.states[this.panel.putNumber + 3]){
    		Counter counter = this.panel.countStone();
	    //一々盤面を走査するのを避けるために白黒の石の数でまず走査
    		if(st.blackCount == counter.blackCount){
    			if(st.whiteCount == counter.whiteCount){
		    //盤面が一致するかを走査；
    				for(int y = 0; y < 8; y++){
    					for(int x = 0; x < 8; x++){
    						if(st.getBoard(y, x) != this.panel.getBoard(y, x)){ continue stateChange; }
    					}
    				}
		    //一致したらそのstateを返す
    				reuseCount++;
    				return st;
    			}
    		}
    	}
	//初めての場合は新しく作成・追加
    	State state = new State(this.panel);
    	return state;
    }
    
    //試合終了時の処理
    public void endMatchProcess(){
    	//1手目はランダムのままにするために削除（対称盤面も含め8つ）
    	for(int i = 0; i < 8; i++){
    		this.tmpSAs.remove(0);
    	}
    	//報酬を計算して追加
    	this.calculateReword();
    	this.doneSAs.addAll(this.tmpSAs);
    	//次の試合の為に空にしておく
    	this.tmpSAs.clear();
    }

    //今は,勝てた場合に+100の報酬を与えることにする
    public void calculateReword(){
    	if(this.panel.gameState == MainPanel.YOU_WIN){
    		for(StateAction sa : this.tmpSAs){
    			sa.doneAction.reward = 100;
    		}
    	}
    }

    //エピソード終了時の処理
    public void endEpisodeProcess(){
    	this.integrateDoneSAs();	//（1）データ統合
    	this.removeUselessData();	//（2）不要データ削除
    	this.evaluate();			//（3）価値関数の算出    
    	this.updatePolicy();		//（4）価値関数から政策改善
    	this.reset();				//（5）データセットを空に
    }		
    //（1）データ統合：doneSAsについて、同じ行動をとった場合その報酬と回数を加算していく→storedSAsへ
    private  void integrateDoneSAs(){
    	processedCount = 0;
    	finCount = doneSAs.size();
    	//forでdoneSAsとstoredSAsを走査
    	done: for(StateAction doneSA : doneSAs){
    		processedCount++;
    		if(processedCount%5000 == 0){
    			this.panel.update(this.panel.getGraphics());
    		}
    		StateAction tmpSA;
    		int stoneCount = doneSA.state.blackCount + doneSA.state.whiteCount;

    		store: for(StateAction storedSA : (ArrayList<StateAction>)storedSAs[stoneCount - 1]){
		//盤面走査の前に白黒の数を比較し、計算を減らす
    			if(doneSA.state.blackCount == storedSA.state.blackCount){if(doneSA.state.whiteCount == storedSA.state.whiteCount){
			//盤面比較
    				for(int x = 0; x < 8; x++){for(int y = 0; y < 8; y++){
				//不一致なものがあった時点で次のstoredSAへ
    					if(doneSA.state.getBoard(x, y) != storedSA.state.getBoard(x, y)){continue store;}
    				}}
			//全ての条件を満たしたので一致、これを上書きする
    				tmpSA = storedSA;
    				for(Action action : tmpSA.state.actions){
			    //行動が一致していれば、報酬と回数を更新する
    					if(action.isSamePointWith(doneSA.doneAction)){
    						action.count++;
    						action.sumOfReward += doneSA.doneAction.reward;
    						storedSA.state.rewards += doneSA.doneAction.reward;
    						continue done;
    					}
    				}
    			}}
    		} 
	    //一致するものがなかったので新しく追加
    		for(Action action : doneSA.state.actions){
		//行動が一致していれば、報酬と回数を更新する
    			if(action.isSamePointWith(doneSA.doneAction)){
    				action.count++;
    				action.sumOfReward += doneSA.doneAction.reward;
    				doneSA.state.rewards += doneSA.doneAction.reward;
    			}
    		}
    		this.storedSAs[stoneCount - 1].add(doneSA);
    	}
    	this.panel.update(this.panel.getGraphics());
    }

    //（2）不要データ削除：一度も勝てていない＝利用できない状態を取り除いてやる
    private void removeUselessData(){
    	// for(int i = this.storedSAs.size() - 1; i >= 0; i--){
    	// 	if(this.storedSAs.get(i).state.rewards == 0) this.storedSAs.remove(i);
    	// }
    	for(int i = 0; i < 64; i++){
    		for(int j = ((ArrayList<StateAction>)this.storedSAs[i]).size() - 1; j >=0; j--){
    			if(((ArrayList<StateAction>)this.storedSAs[i]).get(j).state.rewards == 0) ((ArrayList<StateAction>)this.storedSAs[i]).remove(j);
    		}
    	}
    }

    //（3）価値関数の算出
    private  void evaluate(){
    	processedCount = 0;
    	finCount = 0;
    	for(int i = 0; i < 64; i++){
    		finCount += ((ArrayList<StateAction>)this.storedSAs[i]).size();
    	}
    	for(int i = 0; i < 64; i++){
    		for(StateAction sa : (ArrayList<StateAction>)storedSAs[i]){
    			processedCount++;
    			if(processedCount%300 == 0){
    				this.panel.update(this.panel.getGraphics());
    			}
	    Action bestAction = new Action();		//価値が最大の行動
	    double higherReward = Double.MIN_VALUE;	//より高い方の価値
	    //価値が最大の行動を探索
	    for(Action action : sa.state.actions){
	    	if(action.count != 0){
		    action.averageReward = action.sumOfReward / action.count; //価値関数の計算
		    if(action.averageReward > higherReward){
		    	bestAction = action;
		    	higherReward = action.averageReward;
		    }
		    action.reset(); //座標と確率以外をリセット
		}
	}
	    //ε-greedy法により、政策を決定していく
	double adjustiveValue = this.epsilon / sa.state.actions.size();
	for(Action action : sa.state.actions){
		if(action == bestAction){
			action.prob = 1 - this.epsilon + adjustiveValue;
			sa.state.bestAction = action;
		}else{
			action.prob = adjustiveValue;
		}
	}
}
}
this.panel.update(this.panel.getGraphics());
}
    //（4）価値関数から政策改善
private  void updatePolicy(){
	State state;
	processedCount = 0;
	finCount = 0;
	for(int i = 0; i < 64; i++){
		finCount += ((ArrayList<StateAction>)this.storedSAs[i]).size();
	}
	for(int i = 0; i < 64; i++){
		changeStoredSA: for(StateAction storedSA : (ArrayList<StateAction>)this.storedSAs[i]){

		//プロセス数を表示（300毎）
			processedCount++;
			if(processedCount%300 == 0){
				this.panel.update(this.panel.getGraphics());
			}

			changeState: for(State st : (ArrayList<State>)this.states[i]){
		//白黒の石の数が一致するかどうか調べる
				if(storedSA.state.blackCount == st.blackCount){if(storedSA.state.whiteCount == st.whiteCount){
			//盤面のそれぞれのマス目の状態が一致するか走査		
					for(int x = 0; x < 8; x++){for(int y = 0; y < 8; y++){
						if(storedSA.state.getBoard(x, y) != st.getBoard(x, y)){continue changeState;}
					}}
			st = storedSA.state; //状態が一致したならば新しいものを代入
			continue changeStoredSA;
		}}
	}
	    //同じ状態が見つからなければ、新しく追加する
	this.states[i].add(storedSA.state);
}
}
this.panel.update(this.panel.getGraphics());
}

    //（5）データセットを空に
private  void reset(){
	processedCount = 0;
	finCount = 0;
	this.doneSAs.clear();
	for(int i = 0; i < 64; i++){
		((ArrayList<StateAction>)this.storedSAs[i]).clear();
	}	
}
    //↓↓↓↓対称性への対応↓↓↓↓

    //対称な盤面を作成
    //xとyを入れ替えたもの
private State stateReplaceXY(State preState){
	State state = new State();
	for(int y = 0; y < MASU; y++){
		for(int x = 0; x < MASU; x++){
		//xとyの入れ替え
			state.setStone(x, y, preState.getStone(y, x));
		}
	}
	//石の数は同じ
	state.whiteCount = preState.whiteCount;
	state.blackCount = preState.blackCount;

	state.actions = new ArrayList<Action>();
	for(Action action : preState.actions){
	    //アクションもxとyを入れ替えたものを追加していく
		state.actions.add(new Action(action.getY(), action.getX()));
	}

	state.levelActionProb();
	return state;
}

    //左右対称
private State stateReverseX(State preState){
	State state = new State();
	for(int y = 0; y < MASU; y++){
		for(int x = 0; x < MASU; x++){
		//状態を左右対称に
			state.setStone(MASU - x - 1, y, preState.getStone(x, y));
		}
	}
	//石の数は同じ
	state.whiteCount = preState.whiteCount;
	state.blackCount = preState.blackCount;

	state.actions = new ArrayList<Action>();
	for(Action action : preState.actions){
	    //アクションも左右対称に
		state.actions.add(new Action(MASU - action.getX() - 1, action.getY()));
	}

	state.levelActionProb();
	return state;
}

    //上下対称
private State stateReverseY(State preState){
	State state = new State();
	for(int y = 0; y < MASU; y++){
		for(int x = 0; x < MASU; x++){
		//上下対称に
			state.setStone(x, MASU - y - 1, preState.getStone(x, y));
		}
	}
	//石の数は同じ
	state.whiteCount = preState.whiteCount;
	state.blackCount = preState.blackCount;

	state.actions = new ArrayList<Action>();
	for(Action action : preState.actions){
	    //アクションも上下対称に
		state.actions.add(new Action(action.getX(), MASU - action.getY() - 1));
	}

	state.levelActionProb();
	return state;
}

    //対称な行動の作成
    //xとyを入れかえ
private Action actionReplaceXY(Action action){return (new Action(action.getY(), action.getX()));}
    //左右対称
private Action actionReverseX(Action action){return (new Action( 7 - action.getX(), action.getY()));}
    //上下対称
private Action actionReverseY(Action action){return (new Action(action.getX(), 7 - action.getY()));}

    //対称移動させたものを一括に扱う
    //対称な状態・行動を追加
private void addOtherSAs(StateAction preSA){
     	//コメントは元の状態（preSA）との比較
     	//右斜めに線対称
	this.tmpSAs.add(new StateAction(this.stateReplaceXY(preSA.state), this.actionReplaceXY(preSA.doneAction)));
     	//左右入れ替え
	StateAction sa = new StateAction(this.stateReverseX(preSA.state), this.actionReverseX(preSA.doneAction));
	this.tmpSAs.add(sa);
     	//左90度回転
	this.tmpSAs.add(new StateAction(this.stateReplaceXY(sa.state), this.actionReplaceXY(sa.doneAction)));
     	//上下入れかえ
	StateAction sa0 = new StateAction(this.stateReverseY(preSA.state), this.actionReverseY(preSA.doneAction));
	this.tmpSAs.add(sa0);
     	//右90度
	this.tmpSAs.add(new StateAction(this.stateReplaceXY(sa0.state), this.actionReplaceXY(sa0.doneAction)));
     	//半回転
	StateAction sa1 = new StateAction(this.stateReverseX(sa0.state), this.actionReverseX(sa0.doneAction));
	this.tmpSAs.add(sa1);
     	//左斜めに線対称
	this.tmpSAs.add(new StateAction(this.stateReplaceXY(sa1.state), this.actionReplaceXY(sa1.doneAction)));
}

    //↑↑↑↑対称性への対応↑↑↑↑

    //打つ
private void put(int x, int y){
	// 戻せるように記録しておく
	Undo undo = new Undo(x, y);
	// その場所に石を打つ
	panel.putDownStone(x, y, false);
	// ひっくり返す
	panel.reverse(undo, false);
	// 終了したか調べる
	panel.endGame();
	// 手番を変える
	panel.nextTurn();
}

    //SO:ここから下は単にランダムに置くだけのプログラム（使ってない）
    //石が置ける場所を蓄える
public ArrayList<Point> getCanPutPoint(){
	ArrayList<Point> positionList = new ArrayList<Point>();

	for (int y = 0; y < MainPanel.MASU; y++){
		for (int x = 0; x < MainPanel.MASU; x++) {

		//石が置ける位置を蓄える
			if(panel.canPutDown(x,y)){
				positionList.add(new Point(x,y));
			}
		}
	}
	return positionList;
}

    //ポイントの配列から一つをランダムで選ぶ
public Point randomPoint(ArrayList<Point> positionList){
	Point point = new Point();
	//打てる場所の数がある時、位置を獲得
	if(positionList.size() != 0){
		int size = positionList.size();
	    //サイズが1の時（一応負数の時もカバー）
		if(size<2){
			point = positionList.get(0);
		}else{
			Random rand = new Random();
			point = positionList.get((rand.nextInt(size)));
		}
	}
	return point;
}

public void putRandom(){
	ArrayList<Point> positionList = this.getCanPutPoint();
	if(positionList.size() != 0){
	    //打てる場所からランダムに取得
		Point point = this.randomPoint(positionList);
		int x = point.x;
		int y = point.y;

		this.put(x, y);
	}

}	

    //↓↓↓↓ファイル管理↓↓↓↓
public void inputCSV(){
	// //ファイルを読み込む
	// try{
	//     FileReader inFile = new FileReader("./result/result.csv");
	//     BufferedReader inBuffer = new BufferedReader(inFile);

	//     String line;
	//     String[] sp;
	//     int i ;
	//     while ((line = inBuffer.readLine()) != null) {
	// 	i = 0;
	// 	sp = line.split(",");
	// 	State st = new State();  
	// 	for(int y = 0; y < 8; y++){ 
	// 	    for(int x = 0; x < 8; x++){
	// 		st.setStone(x, y, Integer.valueOf(sp[i++]));
	// 	    }
	// 	}
	// 	st.bestAction = new Action(Integer.valueOf(sp[i++]),Integer.valueOf(sp[i++]));
	// 	st.whiteCount = Integer.valueOf(sp[i++]);
	// 	st.blackCount = Integer.valueOf(sp[i++]);

	// 	st.actions = new ArrayList<Action>();
	// 	while(i <= sp.length - 1){
	// 	    Action act = new Action(Integer.valueOf(sp[i++]),Integer.valueOf(sp[i++]));
	// 	    act.prob = Double.valueOf(sp[i++]);
	// 	    st.actions.add(act);
	// 	}

	// 	this.states.add(st);
	//     }
	// }catch(IOException e){
	// }

	// System.out.printf("現在の状態数：%d\n", this.states.size());
}

public void inputCSV(String title1){

	ArrayList[] tmpStates = new ArrayList[64];
    	for(int i = 0; i < 64; i++){
    		tmpStates[i] = new ArrayList<State>();
    	}

	//ファイルを読み込む
	try{
		String path1 = "./result/" + title1; 
		FileReader inFile = new FileReader(path1);
		BufferedReader inBuffer = new BufferedReader(inFile);

		String line;
		String[] sp;

		//何個目の数値か
		int i ;
		while ((line = inBuffer.readLine()) != null) {
			i = 0;
			sp = line.split(",");
			//盤面
			State st = new State();  
			for(int y = 0; y < 8; y++){ 
				for(int x = 0; x < 8; x++){
					st.setStone(x, y, Integer.valueOf(sp[i++]));
				}
			}
			//最善手と石の数
			st.bestAction = new Action(Integer.valueOf(sp[i++]),Integer.valueOf(sp[i++]));
			st.whiteCount = Integer.valueOf(sp[i++]);
			st.blackCount = Integer.valueOf(sp[i++]);


			int stoneCount = st.whiteCount + st.blackCount;
			//打てる手とその確率
			st.actions = new ArrayList<Action>();
			while(i <= sp.length - 1){
				Action act = new Action(Integer.valueOf(sp[i++]),Integer.valueOf(sp[i++]));
				act.prob = Double.valueOf(sp[i++]);
				st.actions.add(act);
			}

			tmpStates[stoneCount - 1].add(st);
		}
	}catch(IOException e){
	}

	//被りの消去
	processedCount = 0;
    finCount = 0;
    for(int i = 0; i < 64; i++){
    	finCount += tmpStates[i].size();
    }
	for(int i = 0; i < 64; i++){
		changeTemp: for(State tmpState : (ArrayList<State>)tmpStates[i]){
			processedCount++;
    		if(processedCount%5000 == 0){
    			this.panel.update(this.panel.getGraphics());
    		}
			changeState: for(State state : (ArrayList<State>)this.states[i]){
				if(tmpState.blackCount == state.blackCount){if(tmpState.whiteCount == state.whiteCount){
					for(int x = 0; x < 8; x++){for(int y = 0; y < 8; y++){
						if(tmpState.getBoard(x, y) != state.getBoard(x, y)){ continue changeState;}
					}}
					state = tmpState;
					continue changeTemp;
				}}
			}
			this.states[i].add(tmpState);
		}
	}

	int sum = 0;
	System.out.println("\nーーーー石の数ごとにおける保存盤面数ーーーー");
	for(int i = 0; i <64; i++){
		System.out.printf("石の数：%02d, 保存盤面数：%6d\n", i + 1, this.states[i].size());
		sum += this.states[i].size();
	}
	System.out.printf("\n保存盤面総数：%d\n\n", sum);

	processedCount = 0;
    finCount = 0;
	this.panel.update(this.panel.getGraphics());
}

public void outputCSV(){
	// try {
	//     //出力先を作成する

	//     FileWriter fw = new FileWriter("./result/result.csv", false);
	//     PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

	//     //内容を指定する
	//     for(State st : this.states){
	// 	for(int y = 0; y < 8; y++){
	// 	    for(int x = 0; x < 8; x++){
	// 		pw.print(st.getBoard(x, y) + ",");
	// 	    }
	// 	}
	// 	pw.print(st.bestAction.getX() + ",");
	// 	pw.print(st.bestAction.getY() + ",");
	// 	pw.print(st.whiteCount + ",");
	// 	pw.print(st.blackCount + ",");

	// 	String str = "";
	// 	for(Action ac : st.actions){
	// 	    str += ac.getX() + "," + ac.getY() + "," + ac.prob + ",";
	// 	}
	// 	//長さ0はあり得ない筈が、、、
	// 	if(str.length() != 0){
	// 	    pw.print(str.substring(0, str.lastIndexOf(","))); //最後のカンマを取り除いて追加
	// 	}
	// 	pw.println();
	//     }
	//     //ファイルに書き出す
	//     pw.close();

	// } catch (IOException ex) {
	//     //例外時処理
	//     ex.printStackTrace();
	// }
}
public void outputCSV(String title2){
	try {
	    //出力先を作成する
		String path2 = "./result/" + title2;
		FileWriter fw = new FileWriter(path2, false);
		PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

	    //内容を指定する
		for(int i = 0; i < 64; i++){
			for(State st : (ArrayList<State>)this.states[i]){
				
				//盤面の保存
				for(int y = 0; y < 8; y++){
					for(int x = 0; x < 8; x++){
						pw.print(st.getBoard(x, y) + ",");
					}
				}
				
				//最善手と白黒の数の保存
				pw.print(st.bestAction.getX() + ",");
				pw.print(st.bestAction.getY() + ",");
				pw.print(st.whiteCount + ",");
				pw.print(st.blackCount + ",");

				//選択可能手のマスと確率
				String str = "";
				for(Action ac : st.actions){
					str += ac.getX() + "," + ac.getY() + "," + ac.prob + ",";
				}
				//長さ0はあり得ない筈が、、、
				if(str.length() != 0){
				    pw.print(str.substring(0, str.lastIndexOf(","))); //最後のカンマを取り除いて追加
				}
				pw.println();
			}
		}
			    //ファイルに書き出す
		pw.close();

	} catch (IOException ex) {
	    //例外時処理
		ex.printStackTrace();
	}
	int sum = 0;
	for(int i = 0; i <64; i++){
		sum += this.states[i].size();
	}
	System.out.printf("\n保存盤面総数：%d\n\n", sum);
}

    //↑↑↑↑ファイル管理終了↑↑↑↑

public static void main(String[] args) {
	RLAI rlai = new RLAI();
	rlai.inputCSV(args[0]);
	rlai.outputCSV(args[1]);
}
}
