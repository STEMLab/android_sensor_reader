package io.github.stemlab.androidsensorreader.pojo;

public class Globals {
    private static Globals instance;

    // Global variable
    private int data = 1;

    // Restrict the constructor from being instantiated
    private Globals() {
    }

    public static synchronized Globals getInstance() {
        if (instance == null) {
            instance = new Globals();
        }
        return instance;
    }

    public int getData() {
        return this.data;
    }

    public void setData(int d) {
        this.data = d;
    }
}