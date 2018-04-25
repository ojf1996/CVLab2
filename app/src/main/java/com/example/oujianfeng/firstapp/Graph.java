package com.example.oujianfeng.firstapp;

import java.util.*;
import android.util.Log;

public class Graph {
    private double delta = 25;
    private double lambda = 40;

    private int rows;
    private int cols;

    //用来保存像素顶点
    private Node[][] graph;
    //两个terminal
    private Node S;
    private Node T;

    private Vector<Node> activeNodes = new Vector<Node>();
    private Vector<Node> orphans = new Vector<Node>();

    //保存图各边的权重
    //private Map<String, Double> weight = new HashMap<String, Double>();
    //保存图各边的剩余权重
    private Map<String, Double> residualWeight = new HashMap<String, Double>();

    //用来保存整张图片的灰度值
    private int[][] pixels;

    //用来保存种子的灰度值
    private Vector<Integer> S_SeedsPixel = new Vector<Integer>();
    private Vector<Integer> T_SeedsPixel = new Vector<Integer>();


    private Vector<Integer> path = new Vector<Integer>();

    public Graph(int rows, int cols, int[][] pixels) {
        Initialize(rows, cols, pixels);
    }

    //初始化图
    private void Initialize(int rows, int cols, int[][] pixels) {
        this.rows = rows;
        this.cols = cols;

        this.S = new Node(0, -1, -1);
        this.S.SetTreeType(TreeType.S);
        this.S.SetState(State.Passive);

        this.T = new Node(rows - 1, cols, rows * cols);
        this.T.SetTreeType(TreeType.T);
        this.T.SetState(State.Passive);

        this.pixels = pixels;

        graph = new Node[rows][cols];
        for(int y = 0; y < cols; y++) {
            for(int x = 0; x < rows; x++) {
                graph[x][y] = new Node(x, y, x * cols + y);
            }
        }
    }

    //指定种子点
    public void SetSeeds(int x, int y, TreeType type) {
        Node node = this.graph[x][y];

        node.SetTreeType(type);
        int pixel = pixels[x][y];

        if(type == TreeType.S) {
            node.SetParent(S);
            node.SetState(State.Active);
            this.S_SeedsPixel.addElement(pixel);
        }else {
            if(type == TreeType.T) {
                node.SetParent(T);
                node.SetState(State.Active);
                this.T_SeedsPixel.addElement(pixel);
            }else {
                System.out.println("Error in SetSeeds");
                return;
            }
        }

        //更新当前active node
        //activeNodes.addElement(node);
        this.AddActiveNode(node, "SetSeeds");
        //node = null;
    }

    //计算图各边的权重
    public void CalculateWeight() {
        String key;
        double weight;


        double maxNlinkWeight = 0;
        for(int x = 0; x < this.rows; x++) {
            Log.v("","n-link");
            for(int y = 0; y < this.cols; y++) {
                int self = x * cols + y;
                int right = x * cols + y + 1;
                int down = (x + 1) * cols + y;

                //计算右边权重
                if(y + 1 < cols) {
                    key = self + "-" + right;
                    weight = CalculateNlinkWeight(x, y, x, y + 1);
//					this.weight.put(key, weight);
                    this.residualWeight.put(key, weight);

                    if(weight > maxNlinkWeight) {
                        maxNlinkWeight = weight;
                    }
                }

                //计算下边权重
                if(x + 1 < rows) {
                    key = self + "-" + down;
                    weight = CalculateNlinkWeight(x, y, x + 1, y);
//					this.weight.put(key, weight);
                    this.residualWeight.put(key, weight);

                    if(weight > maxNlinkWeight) {
                        maxNlinkWeight = weight;
                    }
                }
            }
        }

        double K = 1 + maxNlinkWeight;
        int pos_S = this.S.GetPosition();
        int pos_T = this.T.GetPosition();
        int pos;
        double t_weight;
        Node node;

        int pixel_S = GetWeightedPixel(S_SeedsPixel);
        int pixel_T = GetWeightedPixel(T_SeedsPixel);

        //计算t-link权重
        for(int x = 0; x < this.rows; x++) {
            for(int y = 0; y < this.cols; y++) {
                node = graph[x][y];
                pos = node.GetPosition();
                if(node.GetTreeType() == TreeType.S) {
                    key = pos_S + "-" + pos;
//					this.weight.put(key, K);
                    this.residualWeight.put(key, K);

                    key = pos + "-" + pos_T;
//					this.weight.put(key, 0d);
                    this.residualWeight.put(key, 0d);
                }else if(node.GetTreeType() == TreeType.T) {
                    key = pos_S + "-" + pos;
//					this.weight.put(key, 0d);
                    this.residualWeight.put(key, 0d);

                    key = pos + "-" + pos_T;
//					this.weight.put(key, K);
                    this.residualWeight.put(key, K);
                }else {
                    key = pos_S + "-" + pos;
                    t_weight = CalculateTlinkWeight(x, y, TreeType.T, pixel_S, pixel_T);
//					this.weight.put(key, t_weight);
                    this.residualWeight.put(key, t_weight);

                    key = pos + "-" + pos_T;
                    t_weight = CalculateTlinkWeight(x, y, TreeType.S, pixel_S, pixel_T);
//					this.weight.put(key, t_weight);
                    this.residualWeight.put(key, t_weight);
                }
            }
        }
    }

    //N-link权重计算函数
    private double CalculateNlinkWeight(int px, int py, int qx, int qy){
        double dist = (double)(Math.abs(pixels[px][py] - pixels[qx][qy]));
        double index = - dist * dist / (2 * delta * delta);

        return Math.exp(index) / dist;
    }

    private double CalculateTlinkWeight(int x, int y, TreeType type, int pixel_S, int pixel_T) {
//        int pixel_S = GetWeightedPixel(S_SeedsPixel);
//        int pixel_T = GetWeightedPixel(T_SeedsPixel);
        int pixel = pixels[x][y];
        int deltaS = Math.abs(pixel_S - pixel);
        int deltaT = Math.abs(pixel_T - pixel);
        int length = deltaS + deltaT;

        if(type == TreeType.S) {
            return -1.0 * lambda * Math.log( deltaS / length);
        }else {
            return -1.0 * lambda * Math.log( deltaT / length);
        }
    }

    private int GetWeightedPixel(Vector<Integer> pixels) {
        int num = pixels.size();
        float sum = 0;
        for(int i = 0; i < num; i++) {
            sum += (float)pixels.get(i) / num;
        }

        return Math.round(sum);
    }

    //获得active node集合
    public Vector<Node> GetActiveNodes() {
        return this.activeNodes;
    }

    //选择活跃点进行处理
    public Node PickActiveNode() {
        if(this.activeNodes.size() > 0) {
            return this.activeNodes.get(0);
        }else {
            System.out.println("Error, activeNode is empty");
            return null;
        }
    }

    public void RemoveActiveNode(Node node) {
        node.SetState(State.Passive);
        this.activeNodes.remove(node);

//		for(int i = 0; i < this.activeNodes.size(); i++) {
//			if(this.activeNodes.get(i).GetPosition() == node.GetPosition()) {
//				Node removednode = this.activeNodes.get(i);
//				//this.activeNodes.removeElementAt(i);
//				this.activeNodes.remove(node);
//				node.SetState(State.Passive);
//				System.out.println("node: " + node.GetPosition() + "; removed node: " + removednode.GetPosition());
//				break;
//			}
//		}
//
//		System.out.println("RemoveActiveNodes:");
//		for(int i = 0; i < this.activeNodes.size(); i++) {
//			System.out.println(this.activeNodes.get(i).GetPosition() + " " + this.activeNodes.get(i).GetTreeType() + " " + this.activeNodes.get(i).GetState());
//		}
    }

    //添加活跃点
    public void AddActiveNode(Node node, String source) {
        //System.out.println("AddActiveNode from + " + source + ": " + node.GetPosition() + " " + node.GetTreeType() + " " + node.GetState());
//
//		if(node.GetTreeType() == TreeType.Free) {
//			System.out.println("ActiveNodes:");
//			for(int i = 0; i < this.activeNodes.size(); i++) {
//				System.out.println(this.activeNodes.get(i).GetPosition() + " " + this.activeNodes.get(i).GetTreeType() + " " + this.activeNodes.get(i).GetState());
//			}
//		}

        node.SetState(State.Active);
        this.activeNodes.add(node);
    }

    //返回node的所有邻居
    public Vector<Node> GetAllNeighbors(Node node) {
        Vector<Node> neighbors = new Vector<Node>();
        int x = node.GetPosX();
        int y = node.GetPosY();

        int right = y + 1;
        int left = y - 1;
        int up = x - 1;
        int down = x + 1;

        //访问左边邻居
        if(ValidationOfY(left)) {
            neighbors.addElement(graph[x][left]);
        }
        //访问右边邻居
        if(ValidationOfY(right)) {
            neighbors.addElement(graph[x][right]);
        }
        //访问上边邻居
        if(ValidationOfX(up)) {
            neighbors.addElement(graph[up][y]);
        }
        //访问下边邻居
        if(ValidationOfX(down)){
            neighbors.addElement(graph[down][y]);
        }

        return neighbors;
    }

    private boolean ValidationOfX(int x) {
        return x <= this.rows - 1 && x >= 0;
    }

    private boolean ValidationOfY(int y) {
        return y <= this.cols - 1 && y >= 0;
    }

    //返回两个node之间的剩余权重
    public double GetResidualWeight(Node start, Node end) {
        int startPos = start.GetPosition();
        int endPos = end.GetPosition();

        return GetResidualWeight(startPos, endPos);
    }

    public Map<String, Double> GetResidualWeight(){
        return this.residualWeight;
    }

    //重载
    private double GetResidualWeight(int start, int end) {
        String key = start + "-" + end;

        if(this.residualWeight.containsKey(key)) {
            return this.residualWeight.get(key);
        }else {
            key = end + "-" + start;
            if(residualWeight.containsKey(key)) {
                return this.residualWeight.get(key);
            }else {
                System.out.println("Error in GetResidualWeight, " + key + " not exist");
                return -1;
            }

        }
    }

    //更新剩余权重
    private void SetResidualWeight(int start, int end, double value) {
        String key = start + "-" + end;

        if(this.residualWeight.containsKey(key)) {
            this.residualWeight.put(key, value);
        } else {
            key = end + "-" + start;
            if(residualWeight.containsKey(key)) {
                this.residualWeight.put(key, value);
            }else {
                System.out.println("Error in SetResidualWeight, Key not exist");
            }

        }
    }


    //获得路径
    public Vector<Integer> GetPath(Node p, Node q) {
        path.clear();

        Node left;
        Node right;

        //确保路径从左到右的方向是S->T
        if(p.GetTreeType() == TreeType.S) {
            left = p;
            right = q;
        } else {
            left = q;
            right = p;
        }

        //这里形成了一个环
        while(left != null) {
            path.add(0, left.GetPosition());
            left = left.GetParent();
            //System.out.println("left:" + left.GetPosition());
        }

        while (right != null) {
            path.addElement(right.GetPosition());
            right = right.GetParent();
            //System.out.println("right");
        }

//		left = null;
//		right = null;
        return this.path;
    }

    //获得路径上的瓶颈
    public double GetBottleneck(Vector<Integer> path) {
        double bottleneck = -1;
        double value;

        for(int i = 0; i < path.size() - 1; i++) {
            value = GetResidualWeight(path.get(i), path.get(i + 1));

            if(bottleneck < 0) {
                bottleneck = value;
            }else {
                if(bottleneck > value) {
                    bottleneck = value;
                }
            }
        }

        return bottleneck;
    }

    //更新路径上的瓶颈，并更新孤儿点
    public void UpdateResidualWeight(double bottleneck, Vector<Integer> path) {
        double updatedValue;
        int start;
        int end;

        for(int i = 0; i < path.size() - 1; i++) {
            start = path.get(i);
            end = path.get(i + 1);
            updatedValue = GetResidualWeight(start, end) - bottleneck;

            if(updatedValue < 0) {
                System.out.println("Error in UpdateResidualWeight");
                return;
            }else {
                SetResidualWeight(start, end, updatedValue);
            }

            //如果权重更新之后为0，那么就直接设置orphans
            if(updatedValue == 0) {
                SetOrphans(start, end);
            }
        }
    }

    //设置orphans node
    private void SetOrphans(int left, int right) {
        Node p = GetNodeByPosition(left);
        Node q = GetNodeByPosition(right);

        if(p.GetTreeType() == q.GetTreeType()) {
            if(p.GetTreeType() == TreeType.S) {
                q.SetParent(null);
                this.orphans.add(q);
            }else if(q.GetTreeType() == TreeType.T) {
                p.SetParent(null);
                this.orphans.add(p);
            }else {
                System.out.println("Error in SetOrphans");
                return;
            }
        }

//		p = null;
//		q = null;
    }

    //获得相应位置的node
    private Node GetNodeByPosition(int position) {
        int x = position / this.cols;
        int y = position % this.cols;

        if(this.graph[x][y].GetPosition() == position) {
            return this.graph[x][y];
        }else {
            System.out.println("Error in GetNodeByPosition");
            return null;
        }
    }

    public Vector<Node> GetOrphans() {
        return this.orphans;
    }

    //获取并删除孤儿点
    public Node PickOrphan() {
        return this.orphans.remove(0);
    }

    //对孤儿点进行处理
    public void ProcessOrphan(Node node) {
        Vector<Node> neighbors = GetAllNeighbors(node);
        Node neighbor;

        //为orphan寻找新的valid parent
        for(int i = 0; i < neighbors.size(); i++) {
            neighbor = neighbors.get(i);
            //寻找同一标签下，剩余权重大于0的邻居
            if(neighbor.GetTreeType() == node.GetTreeType() && GetResidualWeight(node, neighbor) > 0) {
                //避免出现互为父子节点的情况，否则在寻路时会因为成环而陷入死循环
//				if(neighbor.GetParent() != null && neighbor.GetParent().GetPosition() != node.GetPosition()) {
//					int pos = GetOrigin(neighbor).GetPosition();
//					//判断源点是不是S或者T，若是，则寻找到新的valid parent，终止处理
//					if(pos == this.S.GetPosition() || pos == this.T.GetPosition()) {
//						node.SetParent(neighbor);
//						return;
//					}
//				}else {
//					System.out.println("circle");
//				}
                if(neighbor.GetParent() != node) {
                    int pos = GetOrigin(neighbor).GetPosition();
                    //判断源点是不是S或者T，若是，则寻找到新的valid parent，终止处理
                    if(pos == this.S.GetPosition() || pos == this.T.GetPosition()) {
                        node.SetParent(neighbor);
                        return;
                    }
                }
//				else {
//					System.out.println("circle");
//				}
            }
        }

        //没有找到新的valid parent
        for(int i = 0; i < neighbors.size(); i++) {
            neighbor = neighbors.get(i);
            //遍历处于同一标签下的邻居
            if(neighbor.GetTreeType() == node.GetTreeType()) {
                //对于剩余权重大于0的邻居，将其添加到activeNodes里面（若该点已经在activeNodes里面，则不添加）
                if(GetResidualWeight(node, neighbor) > 0 && neighbor.GetState() != State.Active) {
                    //System.out.println("ProcessOrphan: " + neighbor.GetPosition() + " " + neighbor.GetTreeType());
                    AddActiveNode(neighbor, "ProcessOrphan");
                }

                //将orphan的children设置为orphan
                if(neighbor.GetParent() != null) {
                    if(neighbor.GetParent().GetPosition() == node.GetPosition()) {
                        neighbor.SetParent(null);
                        this.orphans.add(neighbor);
                    }
                }
            }
        }

        //将node设置为自由点
        node.SetTreeType(TreeType.Free);
        //从activeNode中移除node
        RemoveActiveNode(node);
    }

    private Node GetOrigin(Node node) {
        Node ptr = node;

        while(ptr.GetParent() != null) {
            ptr = ptr.GetParent();
        }

        return ptr;
    }

    public void ShowMask() {
        String str = "";
        for(int x = 0; x < this.rows; x++) {
            for(int y = 0; y < this.cols; y++) {
                TreeType type = this.graph[x][y].GetTreeType();

                if(type == TreeType.S) {
                    str += "S" + ' ';
                }else if(type == TreeType.T) {
                    str += "T" + ' ';
                }else {
                    str += "F" + ' ';
                }
            }
            System.out.println(str);
            str = "";
        }

        System.out.println();
    }

    //返回图片mask
    public boolean[][] GetMask(TreeType type) {
        boolean[][] mask = new boolean[this.rows][this.cols];
        Node node;

        for(int x = 0; x < rows; x++) {
            for(int y = 0; y < cols; y++) {
                node = this.graph[x][y];
                if(node.GetTreeType() == type) {
                    mask[x][y] = true;
                }else {
                    mask[x][y] = false;
                }
            }
        }

        return mask;
    }
}

