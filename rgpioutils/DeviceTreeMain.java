/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rgpioutils;

import java.util.HashMap;
import java.util.HashSet;
import utils.JSONString;

class Device {

    String name;
    int depth;
    float x, y;
    Device upstream;
    HashSet<Device> downstream;
}

class DeviceTree {

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
            d.downstream = new HashSet<Device>();
            nodes.put(name, d);
        }
        return d;
    }

    public void addLink(String name, String upstreamName) {
        Device d;
        Device du;
        d = addDevice(name);
        du = addDevice(upstreamName);

        d.upstream = du;
        du.downstream.add(d);
        System.out.println("adding " + d.name + " to downstream of " + du.name + " : " + du.downstream.size());
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
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    nrDevices[depth]++;
                }
        );
        for (int i = 0; i <= treeDepth; i++) {
            System.out.println(" nr of devices at depth " + i + " = " + nrDevices[i]);
        }
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    float spacing = 0;
                    if (nrDevices[depth] > 1) {
                        spacing = 1.0f / (nrDevices[depth] - 1);
                    }
                    d.x = leftFilled[depth] * spacing;
                    leftFilled[depth]++;
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
        // print all nodes
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    JSONString json = new JSONString();
                    json.addProperty("name", d.name);
                    json.addProperty("x", String.valueOf(d.x));
                    json.addProperty("y", String.valueOf(d.y));
                    System.out.println(json.asString());
                }
        );
        // print all links
        depthFirst(root, 0,
                (Device d, int depth) -> {
                    if (depth > 0) {
                        JSONString json = new JSONString();
                        json.addProperty("name", "link");
                        json.addProperty("x1", String.valueOf(d.x));
                        json.addProperty("y1", String.valueOf(d.y));
                        json.addProperty("x2", String.valueOf(d.upstream.x));
                        json.addProperty("y2", String.valueOf(d.upstream.y));
                        System.out.println(json.asString());
                    }
                }
        );

    }

}

public class DeviceTreeMain {

    public static void main(String[] args) {
        DeviceTree deviceTree = new DeviceTree("RGPIO");
        deviceTree.addLink("n3", "n2");
        deviceTree.addLink("n4", "n2");
        deviceTree.addLink("n5", "n2");
        deviceTree.addLink("n2", "n1");
        deviceTree.addLink("n6", "RGPIO");
        deviceTree.addLink("n7", "RGPIO");
        deviceTree.addLink("n1", "RGPIO");

        deviceTree.depthFirst(deviceTree.root, 0,
                (Device d, int depth) -> {
                    for (int i = 0; i < depth; i++) {
                        System.out.print("-");
                    }
                    System.out.println(" " + d.name);
                }
        );

        deviceTree.breadthFirst(
                (Device d, int depth) -> {
                    for (int i = 0; i < depth; i++) {
                        System.out.print("-");
                    }
                    System.out.println(" " + d.name);
                }
        );

        deviceTree.layout();

    }

}
