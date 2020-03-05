README
内容：
オセロゲームで、用意されたAI(ミニマックス法)に対して強化学習を用いて
勝利できるようなAIを作成

利用方法：
(1)各javaファイルをコンパイル

(2)コマンド
$ java Othello
により起動

(3)起動後の動作
(a)ミニマックス法AIと対決：
盤面の打てる場所に打つと試合が開始する(先行のみ)

(b)強化学習AIとミニマックスAIとの試合を見る(黒：強化学習、白：ミニマックス)
「vsAI」をクリック

(c)強化学習AIと対決：
「vsP」をクリック

(d)学習結果をファイルから読み込む：
各javaファイルと同階層の「result」フォルダに学習済みファイルを格納
「input」ボタンの左のテキストフィールドにファイル名を入力し、「input」ボタンを選択

(f)学習させる：
・いぷしろん：政策決定時にどれだけ最善策をとるかの確率(0<ε<1)
・試行回数　：ある政策のまま何試合行うか。試行回数分の試合が終わると政策改善がなされる
・反復数　　：政策改善を何度行うか。結果として（試行回数）x(反復数)の試合がなされる。
以上のパラメータを設定した後「start」ボタンを選択