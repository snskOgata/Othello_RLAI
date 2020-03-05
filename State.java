import java.io.*;
import java.util.*;
import java.awt.*;
public class State{

    //メンバ変数
    //SO:ここらへんの値はMainPanelのを流用してしまう？
    // マスの数。オセロは8×8マス（AIクラスで使うのでpublic）
    public static final int MASU = 8;
    // 空白
    private static final int BLANK = 0;
    // 黒石
    private static final int BLACK_STONE = 1;
    // 白石
    private static final int WHITE_STONE = -1;

    //盤面
    private int[][] board = new int[MASU][MASU];
    //白黒の数（盤面比較の前にこれで走査）
    public int blackCount = 0;
    public int whiteCount = 0;
    //状態に対しての行動とその確率
    public ArrayList<Action> actions;
    //エピソード終了時にそのエピソードで報酬最大となる手を保存しておく
    public Action bestAction;
    //この状態から得られた報酬の和（これが0ならば不要なデータ）
    public double rewards = 0;

    //初期化
    public State(){}
    //パネルを用いた初期化
    public State(MainPanel panel){
     this.setBoard(panel);
     this.levelActionProb();
 }



    //現在の盤面とそれに対する可能な手をセット
 private void setBoard(MainPanel panel){
     this.actions = new ArrayList<Action>();
     for(int y = 0; y < MASU; y++){
         for(int x = 0; x < MASU; x++){
          this.board[y][x] = panel.getBoard(x, y);
		//白黒の数を保存
          if(board[y][x] == BLACK_STONE){this.blackCount++;}
          else if(board[y][x] == WHITE_STONE){this.whiteCount++;}
		//置ける場所を保存
          if(panel.canPutDown(x, y)){this.actions.add(new Action(x, y));}
      }
  }
}

    //各行動を一様な確率とする。
public void levelActionProb(){
 if(this.actions.size() != 0){
	    double ud = (double)1/actions.size();//一様分布(Uniform Distribution)
	    for(Action action : actions){
          action.prob = ud;
      }
  }
}
    //以上初期化に関して

    //石を置く
public void setStone(int x, int y, int stoneColor){
	this.board[y][x] = stoneColor;
}

public int getStone(int x, int y){
	return this.board[y][x];
}

    //政策（確率）に従って行動を選択する。
public Action selectAction(){
	Random rand = new Random();
	double trigger = rand.nextDouble();
	for(Action action : this.actions){
     trigger -= action.prob;
     if(trigger <= 0){
      return action;
  }
}
	//割り算の関係で0を下回らなかった場合のエラー回避
if(this.actions.size() > 0){
 Action action = this.actions.get(this.actions.size()-1);
 return action;
}
return (new Action(2, 2));
}

    //盤面を取得
public int getBoard(int x, int y){ return this.board[y][x]; }


}
