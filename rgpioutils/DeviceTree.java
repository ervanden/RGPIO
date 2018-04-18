/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rgpioutils;

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
    }

    private Device addDevice(String name) {
        Device d;
        d = nodes.get(name);
        if (d == null) {
            d = new Device();
            d.name = name;
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

        d = addDevice(name);
        du = addDevice(upstreamName);

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

        if (d.upstream != du) {
            topologyChange = true;
            if (d.upstream != null) {
                d.upstream.downstream.remove(d);
            }
            d.upstream = du;
            du.downstream.add(d);
        }

        System.out.print("adding " + d.name + " to downstream of " + du.name + " : " + du.downstream.size());
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
        action.doIt(device, depth);
        for (Device d : device.downstream) {
            depthFirst(d, depth + 1, action);
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

    public void layout() {
        System.out.println(" ----------- layout() -------------");
        
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

        JSONString json = new JSONString();
        json.addProperty("object", "BEGINTREE");
        RGPIO.webSocketServer.sendToAll(json.asString());

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
                        RGPIO.webSocketServer.sendToAll(json1.asString());
                        //                       System.out.println("drawTreeLink(" + json.asString() + ");");
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
                    RGPIO.webSocketServer.sendToAll(json2.asString());
                    //                   System.out.println("drawTreeNode(" + json.asString() + ");");
                }
        );

        json = new JSONString();
        json.addProperty("object", "ENDTREE");
        RGPIO.webSocketServer.sendToAll(json.asString());

    }

}

/*
 public class DeviceTree {

 public static void main(String[] args) {
 boolean topologyChange;
 DeviceTree deviceTree = new DeviceTree("RGPIO");
 topologyChange = deviceTree.addLink("n3", "n2");
 topologyChange = deviceTree.addLink("n4", "n2");
 topologyChange = deviceTree.addLink("n5", "n2");
 topologyChange = deviceTree.addLink("n2", "n1");
 topologyChange = deviceTree.addLink("n6", "RGPIO");
 topologyChange = deviceTree.addLink("n7", "n1");
 topologyChange = deviceTree.addLink("n1", "RGPIO");
  
 topologyChange = deviceTree.addLink("n3", "n2");
 topologyChange = deviceTree.addLink("n4", "n2");
 topologyChange = deviceTree.addLink("n5", "n2");
 topologyChange = deviceTree.addLink("n2", "n1");
 topologyChange = deviceTree.addLink("n6", "RGPIO");
 topologyChange = deviceTree.addLink("n7", "n1");
 topologyChange = deviceTree.addLink("n1", "RGPIO");
 System.out.println("changing topology");
 topologyChange = deviceTree.addLink("n2", "n6");

 }

 }
 */
