package com.example.oujianfeng.firstapp;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Queue;


public class Graph_
{
    //边
    public class arc
    {
        public node head;//这条边指向的节点
        public arc next;//指向节点指向的下一条边
        public arc sister;//这条边反过来

        public short r_cap;//剩余流量

        public arc()
        {
            head = null;
            next = null;
            sister = null;
            r_cap = 0;
        }
    }

    public class node
    {
        public arc first;
        public arc parent;
        public int index;//方便找parent
        public boolean inSink;
        public node()
        {
            first = null;
            parent = null;
            inSink = false;
        }
    }

    int max;
    node[] nodes;//数组
    Queue<node> queue;//bfs必备
    boolean[] visited;

    //构造函数
    public Graph_(int max)
    {
        nodes = new node[max];
        queue = new ArrayDeque<node>();
        this.max = max;
        visited = new boolean[max];
    }

    public void  init()
    {
        for(int i=0;i<max; ++i)
        {
            nodes[i] = new node();
            nodes[i].index = i;
        }
    }

    public void addEdge(int node_i, int node_j, short cap, short rev_cap)
    {
        arc a = new arc();
        arc a_rev = new arc();

        node i = nodes[node_i];
        node j = nodes[node_j];

        a.sister = a_rev;
        a_rev.sister = a;
        a.next = i.first;
        i.first = a;
        a_rev.next = j.first;
        j.first = a_rev;
        a.head = j;
        a_rev.head = i;
        a.r_cap = cap;
        a_rev.r_cap = rev_cap;
    }

    public int maxflow(int s, int t)
    {
        int max_iteration = 500;
        int flow = 0;

        Log.v("start","maxflow");
        while(seekPath(s,t) && --max_iteration > 0)
        {
            Log.v("ts",Integer.toString(max_iteration));
            int pathFlow = Integer.MAX_VALUE;

            node t_ = nodes[t];
            node s_ = nodes[s];

            for(node u = t_; u != s_; u = u.parent.sister.head)
            {

                pathFlow = Math.min(pathFlow,u.parent.r_cap);
            }

            for(node u = t_; u != s_; u = u.parent.sister.head)
            {
                u.parent.r_cap -= pathFlow;
                u.parent.sister.r_cap += pathFlow;

            }
            flow += pathFlow;
        }
        Log.v("end","maxflow");
        return flow;
    }



    public boolean whatSegment(int index)
    {
        return nodes[index].inSink;
    }

    public boolean seekPath(int s, int t)
    {
        //清除标记
        for (int i = 0; i < max; i++)
        {
            visited[i] = false;
            nodes[i].inSink = false;
        }
        queue.clear();

        visited[s] = true;
        queue.add(nodes[s]);


        while(!queue.isEmpty())
        {
            node i = queue.poll();

            for(arc a = i.first; a != null; a = a.next)
            {
                if(!visited[a.head.index])
                {
                    if(a.r_cap > 0)
                    {
                        queue.add(a.head);
                        visited[a.head.index] = true;
                        a.head.parent = a;
                    }
                    else
                    {
                        a.head.inSink = true;
                    }
                }
            }
        }

        return visited[t] == true;
    }


    public boolean[] getVisited()
    {
        return visited;
    }
}
