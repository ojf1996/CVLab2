package com.example.oujianfeng.firstapp;

enum TreeType{
    S, T, Free
}

enum State{
    Active, Passive, None
}

public class Node {
    private int posX;
    private int posY;
    private int position;
    private TreeType treeType;
    private Node parent;
    private State state;

    public Node(int posX, int posY, int position){
        this.posX = posX;
        this.posY = posY;
        this.position = position;

        this.treeType = TreeType.Free;
        this.parent = null;
        state = State.None;
    }


    public int GetPosition() {
        return this.position;
    }

    public int GetPosX() {
        return this.posX;
    }

    public int GetPosY() {
        return this.posY;
    }

    public void SetTreeType(TreeType type) {
        this.treeType = type;
    }

    public TreeType GetTreeType() {
        return this.treeType;
    }

    public void SetParent(Node parent) {
        this.parent = parent;
    }

    public Node GetParent() {
        return this.parent;
    }

    public State GetState() {
        return this.state;
    }

    public void SetState(State state) {
        this.state = state;
    }
}

