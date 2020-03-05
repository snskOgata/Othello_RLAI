import java.io.*;
import java.util.*;
import java.awt.*;

class Action{
    private int x;
    private int y;
    public double prob;
    public int count = 0;
    public double reward = 0;
    public double sumOfReward = 0;
    public double averageReward = 0;

    //初期化
    public Action(){}
    public Action(int x, int y){
	this.x = x;
	this.y = y;
    }

    public int getX(){return this.x;}
    public int getY(){return this.y;}
    public Boolean isSamePointWith(Action action){
        return ((this.x == action.getX()) && this.y == action.getY());
    }
    //不要？
    public void reset(){
        this.count = 0;
        this.reward = 0;
        this.sumOfReward = 0;
        this.averageReward = 0;
    }
}
