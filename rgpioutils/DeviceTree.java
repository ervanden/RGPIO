/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rgpioutils;

import java.util.ArrayList;
import utils.JSONString;
import java.util.HashMap;
import java.util.HashSet;
import rgpio.RGPIO;

class Device {

    String name;
    int depth;
    float x, y;
    Device upstream;
    HashSet<Device> downstream;
    long lastUpdate;
    boolean expired;
}

public class DeviceTree {

    Device root;
    HashMap<String, Device> nodes = new HashMap<>();

    int treeDepth;
    int[] nrDevices;
    int[] leftFilled;

    public DeviceTree(String name) {
        root = addDevice(name);
        nrDevices = new int[100];
        leftFilled = new int[100];
        generateLayout();
        new DeviceAliveThread().start();
    }

    private Device addDevice(String name) {
        Device d;
        d = nodes.get(name);
        if (d == null) {
            d = new Device();
            d.name = name;
            d.expired = false;
            d.upstream = null;
            d.downstream = new HashSet<Device>();
            nodes.put(name, d);
        }
        return d;
    }

    public boolean addLink(String name, String upstreamName) {

        // returns true if adding this link changes the tree
        Device d;
        Device du;
        boolean topologyChange = false;
        long now = System.currentTimeMillis();

        d = nodes.get(name);
        if (d == null) {
            topologyChange = true;
            d = addDevice(name);
        }

        du = nodes.get(upstreamName);
        if (du == null) {
            topologyChange = true;
            du = addDevice(upstreamName);
        }

        d.lastUpdate = now;
        du.lastUpdate = now;

        if (d.upstream != du) {
            topologyChange = true;
            if (d.upstream != null) {
                d.upstream.downstream.remove(d);
            }
            d.upstream = du;
            du.downstream.add(d);
        }

        System.out.print("adding " + d.name + " -> " + du.name);

        // device confirmed presence
        if (d.expired) {
            topologyChange = true;
        }
        d.expired = false;

        if (topologyChange) {
            System.out.println(" : topology change");
        } else {
            System.out.println(" : no topology change");
        }

        return topologyChange;
    }

    public interface DoSomething {

        // do something with device 'd' at depth 'depth'
        public void doIt(Device d, int depth);
    }

    public void depthFirst(Device device, int depth, DoSomething action) {
        if (!device.expired) {
            action.doIt(device, depth);
            for (Device d : device.downstream) {
                depthFirst(d, depth + 1, action);
            }
        }
    }

    public void breadthFirst(DoSomething action) {

        // update 'depth' for all devices
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    System.out.println("Device " + d.name + " depth=" + depth);
                    d.depth = depth;
                }
        );

        // do something for all devices at depth 0, 
        // then for all devices at depth 1 ..
        // until at a certain depth there were no more devices
        boolean nodesLeft = (!nodes.isEmpty());
        int depth = 0;
        while (nodesLeft) {
            int nrDone = 0;
            for (Device d : nodes.values()) {
                if (d.depth == depth) {
                    action.doIt(d, depth);
                    nrDone++;
                }
            }
            nodesLeft = (nrDone > 0);
            depth++;
        }
    }

    public ArrayList<String> generateLayout() {

        ArrayList<String> layout = new ArrayList<>();

        System.out.println(" ----------- layout() -------------");

        for (Device d : nodes.values()) {
            System.out.println(" device " + d.name + " expired=" + d.expired);
        }

        // determine tree depth
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    System.out.println("Device " + d.name + " depth=" + depth);
                    d.depth = depth;
                    if (depth > treeDepth) {
                        treeDepth = depth;
                    }
                }
        );
        System.out.println("Tree depth = " + treeDepth);
        for (int i = 0; i <= treeDepth; i++) {
            nrDevices[i] = 0;
            leftFilled[i] = 0;
        }

        // count the nr of devices at every level; to calculate the spacing
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    nrDevices[depth]++;
                }
        );
        for (int i = 0; i <= treeDepth; i++) {
            System.out.println(" nr of devices at depth " + i + " = " + nrDevices[i]);
        }

        // calculate the coordinates of every node.
        // sub trees are drawn from left to right
        // at every level, the nodes fill from left to right with equal spacing
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    float spacing = 0;
                    if (nrDevices[depth] > 1) {
                        spacing = 1.0f / (nrDevices[depth] - 1);
                        d.x = leftFilled[depth] * spacing;
                        leftFilled[depth]++;
                    } else {
                        // only one device at this depth
                        d.x = 0.5f;
                    }
                    d.y = (float) depth / (float) treeDepth;
                }
        );
        /*
         breadthFirst(
         (Device d, int depth) -> {
         for (int i = 0; i < depth; i++) {
         System.out.print("-");
         }
         if (depth == 0) {
         System.out.println(" " + d.name + "(" + d.x + "," + d.y + ")");
         } else {
         System.out.println(" " + d.name + "(" + d.x + "," + d.y + ") - "
         + d.upstream.name + "(" + d.upstream.x + "," + d.upstream.y + ")");
         }
         }
         );
         */

        JSONString json = new JSONString();
        json.addProperty("object", "BEGINTREE");
        layout.add(json.asString());

        // print all links (before the nodes, because the nodes overlay the links)
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    if (depth > 0) {
                        JSONString json1 = new JSONString();
                        json1.addProperty("object", "TREELINK");
                        json1.addProperty("name", "link");
                        json1.addPropertyFloat("x1", d.x);
                        json1.addPropertyFloat("y1", d.y);
                        json1.addPropertyFloat("x2", d.upstream.x);
                        json1.addPropertyFloat("y2", d.upstream.y);
                        layout.add(json1.asString());
                        System.out.println(json1.asString());
                    }
                }
        );

        // print all nodes
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    JSONString json2 = new JSONString();
                    json2.addProperty("object", "TREENODE");
                    json2.addProperty("name", d.name);
                    json2.addPropertyFloat("x", d.x);
                    json2.addPropertyFloat("y", d.y);
                    layout.add(json2.asString());
                    System.out.println(json2.asString());
                }
        );

        return layout;
    }

    class DeviceAliveThread extends Thread {

        public DeviceAliveThread() {
            super("DeviceAliveThread");
        }

        public void run() {

            // Find the nodes that have not sent a trace since 10 seconds and set them to 'expired'
            boolean topologyChange;
            long now;
            while (true) {

                topologyChange = false;
                now = System.currentTimeMillis();

                for (Device device : nodes.values()) {
                    if (device != root) {
                        if ((now - device.lastUpdate) > 10000) {  // device expired
                            if (!device.expired) {
                                topologyChange = true;
                            }
                            device.expired = true;
                        }
                    }
                }
                if (topologyChange) {
                    System.out.println("generateLayout because device(s) expired");
                    for (String s : generateLayout()) {
                        RGPIO.webSocketServer.sendToAll(s);
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                };
            }

        }

    }

}
