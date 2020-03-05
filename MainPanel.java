/*
 * 作成日: 2004/12/17
 *
 */
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import javax.swing.*;
/**
 * オセロ盤のクラス。
 * @author mori
 *  
 */
public class MainPanel extends JPanel implements MouseListener{
    // マスのサイズ（GRID SIZE）
    private static final int GS = 50;
    // マスの数。オセロは8×8マス（AIクラスで使うのでpublic）
    public static final int MASU = 8;
    // 盤面の大きさ＝メインパネルの大きさと同じ
    private static final int WIDTH = GS * MASU;
    private static final int HEIGHT = WIDTH;
    // 空白
    private static final int BLANK = 0;
    // 黒石
    private static final int BLACK_STONE = 1;
    // 白石
    private static final int WHITE_STONE = -1;
    // 小休止の時間
    private static final int SLEEP_TIME = 75;
    // 終了時の石の数（オセロは8x8-4=60手で終了する）
    private static final int END_NUMBER = 60;
    // ゲーム状態
    private static final int START = 0;
    private static final int PLAY = 1;
    public static final int YOU_WIN = 2;
    public static final int YOU_LOSE = 3;
    public static final int DRAW = 4;

    private int gamePerRepeat = 1000;       //エピソード毎の試合数
    private int repeatEndCount = 100;      //エピソードを何度繰り返すか

    // 盤面
    private int[][] board = new int[MASU][MASU];
    // 白の番か
    private boolean flagForWhite;
    // 打たれた石の数
    public int putNumber;
    // 石を打つ音
    private AudioClip kachi;
    // ゲーム状態
    public int gameState;
    // AI
    private AI ai;
    // RLAI
    private RLAI rlai;
    
    //試合回数
    private int gameCount;
    //勝利回数
    private int wonCount;
    //サイクルの回数
    private int repeat;
    public int vs;
    // 情報パネルへの参照
    private InfoPanel infoPanel;
    public MainPanel(InfoPanel infoPanel) {
        // Othelloでpack()するときに必要
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.infoPanel = infoPanel; 
        // 盤面を初期化する
        initBoard();
        // サウンドをロードする
        kachi = Applet.newAudioClip(getClass().getResource("kachi.wav"));
        // AIを作成
        ai = new AI(this);
	// RLAIを作成
        rlai = new RLAI(this);
	
        this.gameCount = 0;
        this.wonCount = 0;
        this.repeat = 0;

        // マウス操作を受け付けるようにする
        addMouseListener(this);
        // START状態（タイトル表示）
        gameState = PLAY;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 盤面を描く
        drawBoard(g);
        switch (gameState) {
	case PLAY :
	    // 石を描く
	    drawStone(g);
	    // 盤面の石の数を数える
	    Counter counter = countStone();
	    // ラベルに表示
	    infoPanel.setBlackLabel(counter.blackCount);
	    infoPanel.setWhiteLabel(counter.whiteCount);
	    break;
	case YOU_WIN :
	    drawStone(g);
	    if(vs == 2) {
		drawTextCentering(g, "YOU LOSE");
	    } else {
		drawTextCentering(g, "YOU WIN");
	    }
	    break;
	case YOU_LOSE :
	    drawStone(g);
	    if(vs == 2) {
		drawTextCentering(g, "YOU WIN");
	    }else{
		drawTextCentering(g, "YOU LOSE");
	    }
	    break;
	case DRAW :
	    drawStone(g);
	    drawTextCentering(g, "DRAW");
	    break;
	}
	this.drawMatchCount(g);
	this.drawProcess(g);
    }

    /**
     * マウスをクリックしたとき。石を打つ。
     */
    public void mouseClicked(MouseEvent e) {
        switch (gameState) {
	case PLAY :
	    // どこのマスかを調べる
	    int x = e.getX() / GS;
	    int y = e.getY() / GS;

	    // (x, y)に石が打てる場合だけ打つ
	    if (canPutDown(x, y)) {
		// 戻せるように記録しておく
		Undo undo = new Undo(x, y);
		// その場所に石を打つ
		putDownStone(x, y, false);
		// ひっくり返す
		reverse(undo, false);
		// 終了したか調べる
		endGame();
		// 手番を変える
		nextTurn();
		// AIがパスの場合はもう一回
		if (countCanPutDownStone() == 0) {
		    System.out.println("AI PASS!");
		    nextTurn();
		    return;
		} else {
		    // パスでなかったらAIが石を打つ
		    rlai.compute();
		}
	    }
	    break;
	case YOU_WIN :
	    this.wonCount++;
	case YOU_LOSE :
	case DRAW :
	    //盤面初期化
	    initBoard();
	    //ゲーム終了時にクリックされたら0手目へ戻る
	    gameState = PLAY;
	    break;
	}
        // 再描画する
	repaint();
    }
    public void change() {
	initBoard();
	gameState = PLAY;
	rlai.compute();
	repaint();
    }

    public void gameStart() {
	gameState = PLAY;
	initBoard();
	repaint();
	while(true){
	    rlai.eternalForceBlizzard();
	    ai.compute();
	    if(putNumber == END_NUMBER) break;
	}	
	// 再描画する
	repaint();
    }
    public void gameStart(double ep,int count,int rp) {
	rlai.epsilon = ep;
	gamePerRepeat = count;
	repeatEndCount = rp;
	b: while(true){
	    repeat++;
	    System.out.printf("\nRepeat%d\nstart!\n", this.repeat);
	    gameCount = 0;
	    wonCount = 0;
	    rlai.reuseCount = 0;

	    gameState = MainPanel.PLAY;
	    initBoard();

	    a: while(true){
		rlai.compute();
		ai.compute();
		if(gameState == PLAY){
		    continue a;
		}else{
		    if(gameState == YOU_WIN) wonCount++;

		    gameCount++;
		    //ゲーム終了時の盤面を描画
		    update(getGraphics());
		    //データの整理
		    rlai.endMatchProcess();

		    //1反復終了毎の処理
		    if(gameCount >= gamePerRepeat){
			System.out.printf("finish!\n");
			//ターミナル側で試合数と勝率を表示
			double wp = (double)100*wonCount/gameCount;
			System.out.printf("勝率:%6.2f%%\n", wp);
			System.out.printf("再利用された数:%4d\n", rlai.reuseCount);
			//データの編集と政策の改善
			rlai.endEpisodeProcess();
			//int size = rlai.states.size();
			//System.out.printf("保存されたStateの数：%d\n", size);
			break a;
		    }
		    // 盤面初期化、次の試合へ
		    initBoard();
		    gameState = MainPanel.PLAY;

		}
	    }
	    if(repeat >= repeatEndCount){
		repeat = 0;
		System.out.println("学習終了");
		break b;
	    }
	}
        // 再描画する
	repaint();
    }

    /**
     * 盤面を初期化する。
     *  
     */
    private void initBoard() {
        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {
                board[y][x] = BLANK;
            }
        }
        // 初期配置
        board[3][3] = board[4][4] = WHITE_STONE;
        board[3][4] = board[4][3] = BLACK_STONE;

        // 黒番から始める
        flagForWhite = false;
        putNumber = 0;
    }

    /**
     * 盤面を描く。
     * 
     * @param g 描画オブジェクト。
     */
    private void drawBoard(Graphics g) {
        // マスを塗りつぶす
        g.setColor(new Color(0, 128, 128));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {
                // マス枠を描画する
                g.setColor(Color.BLACK);
                g.drawRect(x * GS, y * GS, GS, GS);
            }
        }
    }

    /**
     * 石を描く。
     * 
     * @param g 描画オブジェクト
     */
    private void drawStone(Graphics g) {
        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {
                if (board[y][x] == BLANK) {
                    continue;
                } else if (board[y][x] == BLACK_STONE) {
                    g.setColor(Color.BLACK);
                } else {
                    g.setColor(Color.WHITE);
                }
                g.fillOval(x * GS + 3, y * GS + 3, GS - 6, GS - 6);
            }
        }
    }

    /**
     * 盤面に石を打つ。
     * 
     * @param x 石を打つ場所のx座標。
     * @param y 石を打つ場所のy座標。
     * @param tryAndError コンピュータの思考実験中かどうか。思考中は石を描画しない。
     */
    public void putDownStone(int x, int y, boolean tryAndError) {
        int stone;

        // どっちの手番か調べて石の色を決める
        if (flagForWhite) {
            stone = WHITE_STONE;
        } else {
            stone = BLACK_STONE;
        }
        // 石を打つ
        board[y][x] = stone;
        // コンピュータの思考中でなければ実際に打って再描画する
        if (!tryAndError) {
            putNumber++;
            if(vs != 0){
		// カチッ
		kachi.play();
		// 盤面が更新されたので再描画
		update(getGraphics());
		// 小休止を入れる（入れないとすぐにひっくり返しが始まってしまう）
		sleep();
	    }
	}
    }

    /**
     * 石が打てるかどうか調べる。
     * 
     * @param x 石を打とうとしている場所のx座標。
     * @param y 石を打とうとしている場所のy座標。
     * @return 石が打てるならtrue、打てないならfalseを返す。
     *  
     */
    public boolean canPutDown(int x, int y) {
        // (x,y)が盤面の外だったら打てない
        if (x >= MASU || y >= MASU)
            return false;
        // (x,y)にすでに石が打たれてたら打てない
        if (board[y][x] != BLANK)
            return false;
        // 8方向のうち一箇所でもひっくり返せればこの場所に打てる
        // ひっくり返せるかどうかはもう1つのcanPutDownで調べる
        if (canPutDown(x, y, 1, 0))
            return true; // 右
        if (canPutDown(x, y, 0, 1))
            return true; // 下
        if (canPutDown(x, y, -1, 0))
            return true; // 左
        if (canPutDown(x, y, 0, -1))
            return true; // 上
        if (canPutDown(x, y, 1, 1))
            return true; // 右下
        if (canPutDown(x, y, -1, -1))
            return true; // 左上
        if (canPutDown(x, y, 1, -1))
            return true; // 右上
        if (canPutDown(x, y, -1, 1))
            return true; // 左下

        // どの方向もだめな場合はここには打てない
        return false;
    }

    /**
     * vecX、vecYの方向にひっくり返せる石があるか調べる。
     * 
     * @param x 石を打とうとしている場所のx座標。
     * @param y 石を打とうとしている場所のy座標。
     * @param vecX 調べる方向を示すx方向ベクトル。
     * @param vecY 調べる方向を示すy方向ベクトル。
     * @return 石が打てるならtrue、打てないならfalseを返す。
     *  
     */
    private boolean canPutDown(int x, int y, int vecX, int vecY) {
        int putStone;

        // 打つ石はどれか
        if (flagForWhite) {
            putStone = WHITE_STONE;
        } else {
            putStone = BLACK_STONE;
        }

        // 隣の場所へ。どの隣かは(vecX, vecY)が決める。
        x += vecX;
        y += vecY;
        // 盤面外だったら打てない
        if (x < 0 || x >= MASU || y < 0 || y >= MASU)
            return false;
        // 隣が自分の石の場合は打てない
        if (board[y][x] == putStone)
            return false;
        // 隣が空白の場合は打てない
        if (board[y][x] == BLANK)
            return false;

        // さらに隣を調べていく
        x += vecX;
        y += vecY;
        // となりに石がある間ループがまわる
        while (x >= 0 && x < MASU && y >= 0 && y < MASU) {
            // 空白が見つかったら打てない（はさめないから）
            if (board[y][x] == BLANK)
                return false;
            // 自分の石があればはさめるので打てる
            if (board[y][x] == putStone) {
                return true;
            }
            x += vecX;
            y += vecY;
        }
        // 相手の石しかない場合はいずれ盤面の外にでてしまうのでこのfalse
        return false;
    }

    /**
     * 石をひっくり返す。
     * 
     * @param x 石を打った場所のx座標。
     * @param y 石を打った場所のy座標。
     * @param tryAndError コンピュータの思考実験中かどうか。思考中は石を描画しない。
     */
    public void reverse(Undo undo, boolean tryAndError) {
        // ひっくり返せる石がある方向はすべてひっくり返す
        if (canPutDown(undo.x, undo.y, 1, 0))
            reverse(undo, 1, 0, tryAndError);
        if (canPutDown(undo.x, undo.y, 0, 1))
            reverse(undo, 0, 1, tryAndError);
        if (canPutDown(undo.x, undo.y, -1, 0))
            reverse(undo, -1, 0, tryAndError);
        if (canPutDown(undo.x, undo.y, 0, -1))
            reverse(undo, 0, -1, tryAndError);
        if (canPutDown(undo.x, undo.y, 1, 1))
            reverse(undo, 1, 1, tryAndError);
        if (canPutDown(undo.x, undo.y, -1, -1))
            reverse(undo, -1, -1, tryAndError);
        if (canPutDown(undo.x, undo.y, 1, -1))
            reverse(undo, 1, -1, tryAndError);
        if (canPutDown(undo.x, undo.y, -1, 1))
            reverse(undo, -1, 1, tryAndError);
    }

    /**
     * 石をひっくり返す。
     * 
     * @param x 石を打った場所のx座標。
     * @param y 石を打った場所のy座標。
     * @param vecX ひっくり返す方向を示すベクトル。
     * @param vecY ひっくり返す方向を示すベクトル。
     * @param tryAndError コンピュータの思考実験中かどうか。思考中は石を描画しない。
     */
    private void reverse(Undo undo, int vecX, int vecY, boolean tryAndError) {
        int putStone;
        int x = undo.x;
        int y = undo.y;

        if (flagForWhite) {
            putStone = WHITE_STONE;
        } else {
            putStone = BLACK_STONE;
        }

        // 相手の石がある間ひっくり返し続ける
        // (x,y)に打てるのは確認済みなので相手の石は必ずある
        x += vecX;
        y += vecY;
        while (board[y][x] != putStone) {
            // ひっくり返す
            board[y][x] = putStone;
            // ひっくり返した場所を記録しておく
            undo.pos[undo.count++] = new Point(x, y);
            if (!tryAndError) {

		if(vs != 0){
		    // カチッ
		    kachi.play();
		    // 盤面が更新されたので再描画
		    update(getGraphics());
		    // 小休止を入れる（入れないと複数の石が一斉にひっくり返されてしまう）
		    sleep();
		}
	    }
	    x += vecX;
	    y += vecY;
	}

	if(!tryAndError){
	    if(vs != 0){
		update(getGraphics());
		sleep();
	    }
	}
    }

    /**
     * オセロ盤を1手手前の状態に戻す。 AIは石を打ったり戻したりして盤面を評価できる。
     * 
     * @param undo ひっくり返した石の情報。
     */
    public void undoBoard(Undo undo) {
        int c = 0;

        while (undo.pos[c] != null) {
            // ひっくり返した位置を取得
            int x = undo.pos[c].x;
            int y = undo.pos[c].y;
            // 元に戻すには-1をかければよい
            // 黒(1)は白(-1)に白は黒になる
            board[y][x] *= -1;
            c++;
        }
        // 石を打つ前に戻す
        board[undo.y][undo.x] = BLANK;
        // 手番も元に戻す
        nextTurn();
    }

    /**
     * 手番を変える。
     *  
     */
    public void nextTurn() {
        // 手番を変える
        flagForWhite = !flagForWhite;
    }

    /**
     * 石が打てる場所の数を数える。
     * @return 石が打てる場所の数。
     */
    public int countCanPutDownStone() {
        int count = 0;
        
        for (int y = 0; y < MainPanel.MASU; y++) {
            for (int x = 0; x < MainPanel.MASU; x++) {
                if (canPutDown(x, y)) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * SLEEP_TIMEだけ休止を入れる
     *  
     */
    private void sleep() {
        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 画面の中央に文字列を表示する
     * 
     * @param g 描画オブジェクト
     * @param s 描画したい文字列
     */
    public void drawTextCentering(Graphics g, String s) {
        Font f = new Font("SansSerif", Font.BOLD, 20);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.YELLOW);
        g.drawString(s, WIDTH / 2 - fm.stringWidth(s) / 2, HEIGHT / 2
		     + fm.getDescent());
    }

    public void drawMatchCount(Graphics g){
	Font f = new Font("SansSerif", Font.PLAIN, 12);
	g.setFont(f);
	FontMetrics fm = g.getFontMetrics();
	String str = this.gameCount + "/" + this.gamePerRepeat;
	g.setColor(Color.black);
        g.fillRect(WIDTH - fm.stringWidth(str), HEIGHT - 12, fm.stringWidth(str), 12);
	g.setColor(Color.white);
	g.drawString(str, WIDTH - fm.stringWidth(str), HEIGHT - fm.getDescent());
    }

    public void drawProcess(Graphics g){
    	Font f = new Font("SansSerif", Font.PLAIN, 12);
    	g.setFont(f);
    	FontMetrics fm = g.getFontMetrics();
    	String str = rlai.processedCount + "/" + rlai.finCount;
    	g.setColor(Color.black);
        g.fillRect(0, HEIGHT - 12, fm.stringWidth(str), 12);
    	g.setColor(Color.white);
    	g.drawString(str, 0, HEIGHT - fm.getDescent());
    }
    /**
     * ゲームが終了したか調べる。
     *  
     */
    public boolean endGame() {

        // 打たれた石の数が60個（全部埋まった状態）に勝敗を決める
        if (putNumber == END_NUMBER) {
	    //盤上の石をカウントする
	    Counter counter;
	    counter = countStone();
            // 黒が過半数（64/2=32）を取っていたら勝ち
            // 過半数以下なら負け
            // 同じ数なら引き分け
	    if (counter.blackCount > 32) {
		gameState = YOU_WIN;
	    } else if (counter.blackCount < 32) {
		gameState = YOU_LOSE;
	    } else {
		gameState = DRAW;
	    }
	    repaint();
	    return true;
	}
	return false;
    }

    /**
     * オセロ盤上の石の数を数える
     * 
     * @return 石の数を格納したCounterオブジェクト
     *  
     */
    public Counter countStone() {
        Counter counter = new Counter();

        for (int y = 0; y < MASU; y++) {
            for (int x = 0; x < MASU; x++) {
                if (board[y][x] == BLACK_STONE)
                    counter.blackCount++;
                if (board[y][x] == WHITE_STONE)
                    counter.whiteCount++;
            }
        }

        return counter;
    }

    //rlaiでCSVを読み込む
    public void inputStart(String title1){
	System.out.println("ファイル入力開始");
	rlai.inputCSV(title1);
	System.out.println("ファイル入力完了\n");
    }
    public void outputStart(String title2){
	System.out.println("ファイル出力開始");
	rlai.outputCSV(title2);
	System.out.println("ファイル出力完了\n");
    }
    
    /**
     * (x,y)のボードの石の種類を返す。
     * @param x X座標。
     * @param y Y座標。
     * @return BLANK or BLACK_STONE or WHITE_STONE
     */
    public int getBoard(int x, int y) {
        return board[y][x];
    }

    public void mousePressed(MouseEvent e){}

    public void mouseEntered(MouseEvent e){}

    public void mouseExited(MouseEvent e){}

    public void mouseReleased(MouseEvent e){}
}
