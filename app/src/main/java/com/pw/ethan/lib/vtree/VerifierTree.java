package com.pw.ethan.lib.vtree;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;

public class VerifierTree {
    private ArrayList<Integer> evidence;
    private int depth = 0;
    private int size = 0;
    private int capacity = 0;
    private LinkedList<ArrayList<Integer>> vtree;

    private final String TAG = "HealthCare";

    public VerifierTree() {
        this.depth = 0;
        this.size = 0;
        this.capacity = 0;
        this.evidence = new ArrayList<>();
        this.vtree = new LinkedList<>();
    }

    public boolean updateVTree(ArrayList<Integer> weights) {
        Log.i(TAG, "update vtree");
        if (weights.isEmpty() || weights.size() != PowerTwo(this.depth)) {
            Log.e(TAG, "updateVTree() -- parameter error");
            return false;
        }

        int index = 0;
        ArrayList<Integer> newlayer = new ArrayList<>();
        newlayer.add(weights.get(index++));
        vtree.addFirst(newlayer);

        for (int i = 1; i < vtree.size(); ++i) {
            int sz = vtree.get(i).size();
            for (int j = 0; j < sz; ++j) {
                vtree.get(i).add(weights.get(index++));
            }
        }

        if (evidence.isEmpty()) {
            evidence.add(0);
        } else {
            evidence.add(evidence.get(evidence.size() - 1) * weights.get(0));
        }

        if (0 == capacity) {
            capacity = 1;
        } else {
            capacity *= 2;
        }

        depth += 1;

        print();
        return true;
    }

    public boolean appendValue(int val) {
        Log.d(TAG, "append value : " + val);
        int offset = this.size;
        Integer valueAdd2Evidence = val;
        int i = vtree.size() - 1;
        for (; i > 0; --i) { // there is different from c++ code
            valueAdd2Evidence *= vtree.get(i).get(offset);
            offset /= 2;
        }
        evidence.set(evidence.size() - 1, evidence.get(evidence.size() - 1) + valueAdd2Evidence);
        size += 1;
        print();
        return true;
    }

//    public boolean verify(int index, int data, ArrayList<Integer> auth) {
//        System.out.println("verify");
//        if (index >= size) {
//            System.err.println("verify() -- parameter error");
//            return false;
//        }
//        int offset = index;
//        Integer evi = data;
//        int i = 0;
//        int it = vtree.size() - 1;
//        for (; it > 0; ++it) {
//            evi *= vtree.get(it).get(offset);
//            if (offset % 2 == 1) {
//                evi += auth.get(i++) * vtree.get(it).get(offset - 1);
//            } else {
//                evi += auth.get(i++) * vtree.get(it).get(offset + 1);
//            }
//            offset /= 2;
//        }
//
//        if (evidence.get(evidence.size() - 1) == evi) {
//            return true;
//        }
//
//        return false;
//    }

    public boolean IsFull() {
        return this.capacity == this.size;
    }


    public int getDepth() {
        return depth;
    }

//    public int getEvidence() {
//        return evidence.get(evidence.size() - 1);
//    }

    public void print() {
        Log.i(TAG, "capacity : " + capacity + "\n    size : " + size + "\n   depth : " + depth);
        StringBuilder sb = new StringBuilder();
        sb.append("evidence: ");
        for (Integer i : evidence) {
            sb.append(i);
            sb.append("\t");
        }
        sb.append("\n");
        Log.i(TAG, sb.toString());

        sb = new StringBuilder("tree structure :\n");

        for (ArrayList<Integer> tmp : vtree) {
            for (Integer i : tmp) {
                sb.append(i);
                sb.append("\t");
            }
            sb.append("\n");
        }
        Log.i(TAG, sb.toString());
    }

    private int PowerTwo(int n) {
        return 1 << n;
    }

}
