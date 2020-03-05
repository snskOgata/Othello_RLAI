import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.GridLayout;
import java.io.File;

public class SubPanel extends JPanel implements ActionListener {
    JButton startButton,inputButton,outputButton,vsAIButton,vsPButton,fileButton,fileButton2;
    private JTextField count,ipsrn,repeat,input,output;
    private MainPanel mp;
    JLabel label;
    public SubPanel(MainPanel mainPanel) {
	this.mp = mainPanel;
	setLayout(new GridLayout(8, 3));
	startButton = new JButton("start");
	inputButton = new JButton("input");
	outputButton = new JButton("output");
	vsAIButton = new JButton("vs AI");
	vsPButton = new JButton("vs P");
	fileButton = new JButton("file選択");
	fileButton2 = new JButton("file選択");
	count = new JTextField("1000");
	ipsrn = new JTextField("0.1");
	repeat = new JTextField("1");
	output = new JTextField("result.csv");
	input = new JTextField("result.csv");
	label = new JLabel();

	startButton.addActionListener(this);
	inputButton.addActionListener(this);
	outputButton.addActionListener(this);
	vsAIButton.addActionListener(this);
	vsPButton.addActionListener(this);
	fileButton.addActionListener(this);
	fileButton2.addActionListener(this);

	add(vsAIButton);
	add(new JLabel(""));
	add(vsPButton);

	add(new JLabel(""));
	JLabel learn = new JLabel("学習");
	learn.setHorizontalAlignment(JLabel.CENTER);
	add(learn);
	add(new JLabel(""));

	add(fileButton);
	add(input);
	add(inputButton);

	add(new JLabel("いぷしろん"));
	add(ipsrn);
	add(new JLabel(""));

	add(new JLabel("試行回数"));
	add(count);
	add(new JLabel("回"));

	add(new JLabel("反復数"));
	add(repeat);
	add(new JLabel("回"));

	add(new JLabel("出力ファイル"));
	add(output);
	add(outputButton);

	add(new JLabel(""));
	add(startButton);
    }

    public void actionPerformed(ActionEvent ae){
	JFileChooser filechooser = new JFileChooser();
	if(ae.getSource() == fileButton){
	    int selected = filechooser.showOpenDialog(this);
	    if (selected == JFileChooser.APPROVE_OPTION){
		File file = filechooser.getSelectedFile();
		input.setText(file.getName());
	    }else if (selected == JFileChooser.CANCEL_OPTION){
		label.setText("キャンセルされました");
	    }else if (selected == JFileChooser.ERROR_OPTION){
		label.setText("エラー又は取消しがありました");
	    }
	}

	//JTextField output = new JTextField("初期値");
	//String tex = text.getText();
	
	JFileChooser filechooser2 = new JFileChooser();
	if(ae.getSource() == fileButton2){
	    int selected2 = filechooser2.showOpenDialog(this);
	    if (selected2 == JFileChooser.APPROVE_OPTION){
		File file2 = filechooser2.getSelectedFile();
		output.setText(file2.getName());
	    }else if (selected2 == JFileChooser.CANCEL_OPTION){
		label.setText("キャンセルされました");
	    }else if (selected2 == JFileChooser.ERROR_OPTION){
		label.setText("エラー又は取消しがありました");
	    }
	}
	if (ae.getSource() == startButton){
	    mp.vs=0;
	    double ep = Double.valueOf(ipsrn.getText());
	    int cnt = Integer.valueOf(count.getText());
	    int rp = Integer.valueOf(repeat.getText());
	    //String title1 = input.getText();
	    // String title2 = output.getText();
	    mp.gameStart(ep,cnt,rp);
	}	
	if(ae.getSource() == inputButton){
	    mp.inputStart(input.getText());
	}
	if(ae.getSource() == outputButton){
	    mp.outputStart(output.getText());
	}
	if(ae.getSource() == vsAIButton) {
	    mp.vs=1;
	    mp.gameStart();
	}
	if(ae.getSource() == vsPButton) {
	    mp.vs=2;
	    mp.change();
	}
    }
}
