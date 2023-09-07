// Written by William Barsaloux
// ID# 80697857
// This was coded on Apache NetBeans IDE 16

package com.mycompany.cs143bproject1;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
//import javax.swing.*;

// this class is here because I couldn't import Pairs from javafx
class WaitlistPair {

    int first;
    int second;

    WaitlistPair(int f, int s) {
        first = f;
        second = s;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}

class Process {

    int priority;
    private int state; // 1 = ready state, 0 = blocked state
    private int parent; // index of process that created it
    private final int id;
    int waitingOnResource;
    LinkedList<Integer> children;
    LinkedList<Integer> heldResources;
    LinkedList<Integer> descendants;
    HashMap<Integer, Integer> resources;

    Process(int i) {
        id = i;
        state = 1; // 1 = ready state, 0 = blocked state
        parent = 99; // This is default value in case there is no parent
        children = new LinkedList<>();
        heldResources = new LinkedList<>();
        descendants = new LinkedList<>();
        resources = new HashMap<>();
    }

    public int getID() {
        return id;
    }

    public void setParent(int p) {
        parent = p;
    }

    public int getParent() {
        return parent;
    }

    public void addChild(int c) {
        children.addLast(c);
    }

    public void setStateReady() {
        state = 1;
    }

    public void setStateBlocked(int r) {
        state = 0;
        waitingOnResource = r;
    }

    public boolean isBlocked() {
        return state == 0;
    }

    public void addUnits(int r, int k) {
        if (resources.containsKey(r)) {
            int v = resources.get(r);
            resources.replace(r, v + k);
        } else {
            resources.put(r, k);
            heldResources.addFirst(r);
        }
    }

    public void removeUnits(int r, int k) {
        if (resources.get(r) == k) {
            resources.remove(r);
            heldResources.removeFirstOccurrence(r);
        } else {
            int v = resources.get(r);
            resources.replace(r, v - k);
        }
    }
}

class Resource {

    int state;
    int inventory;
    LinkedList<WaitlistPair> waitlist;

    Resource(int units) {
        state = units;
        inventory = units;
        waitlist = new LinkedList<>();
    }
}

class Manager {

    Process PCB[];
    Resource RCB[];
    LinkedList<Process> RL[];
    int runningProcess;
    int activeProcesses;

    // for file reading and writing
    File inputFile;
    FileWriter outputFile;
    Scanner fileRead;

    Manager() {
        runningProcess = 0;
        activeProcesses = 1;
    }

    void run() {
        try
        {
            inputFile = new File("input.txt");
            outputFile = new FileWriter("output.txt");
            fileRead = new Scanner(inputFile);
            
            while(fileRead.hasNextLine())
            {
                String command = fileRead.nextLine();
                if (command.isBlank())
                {
                    outputFile.write("\n");
                    //outputFile.flush();
                }
                else
                {
                    String result = processCommand(command);
                    
                    // if end of file is reached, add an extra space
                    if (!fileRead.hasNextLine())
                    {
                        outputFile.write(result + " ");
                    }
                    else
                    {
                        outputFile.write(result);
                    }
                }
            }
            outputFile.close();
            fileRead.close();
        }
        catch (IOException e)
        {
            System.out.println("error!");
        }
    }

    String processCommand(String s) {
        String[] command = s.split(" ");
        String output;

        // check commands
        switch (command[0]) {

            // initialize command
            case "in" -> {
                init();
                output = String.valueOf(runningProcess);
            }

            // create command
            case "cr" -> {
                int p = Integer.parseInt(command[1]);
                if (activeProcesses == 16) {
                    //System.out.println("Error! Maximum processes created (-1)");
                    output = " -1";
                } else if (p < 1 || p > 2) {
                    //System.out.println("Error! Priority must be 1 or 2 (-1)");
                    output = " -1";
                } else {
                    create(p);
                    output = " " + String.valueOf(runningProcess);
                }
            }

            // destroy command
            case "de" -> {
                int p = Integer.parseInt(command[1]);
                if (p == 0) {
                    //System.out.println("Error! Cannot destroy process 0 (-1)");
                    output = " -1";
                } else if (p < 0 || p > 15) {
                    //System.out.println("Error! Process must be from 1 to 15 (-1)");
                    output = " -1";
                } else if (PCB[p] == null) {
                    //System.out.println("Error! Process " + p + " does not exist (-1)");
                    output = " -1";
                } else if (p != runningProcess && !PCB[runningProcess].descendants.contains(p)) {
                    //System.out.println("Error! Process " + p + " is not a child of running process " + runningProcess + " (-1)");
                    output = " -1";
                } else {
                    destroy(p);
                    output = " " + String.valueOf(runningProcess);
                }
            }

            // request command
            case "rq" -> {
                int r = Integer.parseInt(command[1]);
                int k = Integer.parseInt(command[2]);

                if (runningProcess == 0) {
                    //System.out.println("Error! Process 0 cannot request any resources (-1)");
                    output = " -1";
                } else if (r < 0 || r > 3) {
                    //System.out.println("Error! Resource " + r + " does not exist! (-1)");
                    output = " -1";
                } else if (k > RCB[r].inventory) {
                    //System.out.println("Error! Requested more units than resource " + r + " contains (-1)");
                    output = " -1";
                } else if (k <= 0) {
                    //System.out.println("Error! Please request a nonzero number of units from a resource (-1)");
                    output = " -1";
                } else if (PCB[runningProcess].resources.containsKey(r) && k + PCB[runningProcess].resources.get(r) > RCB[r].inventory) {
                    //System.out.println("Error! Not enough units in resource " + r + " (-1)");
                    output = " -1";
                } else {
                    request(r, k);
                    output = " " + String.valueOf(runningProcess);
                }
            }

            // release command
            case "rl" -> {
                int r = Integer.parseInt(command[1]);
                int k = Integer.parseInt(command[2]);

                if (!PCB[runningProcess].resources.containsKey(r)) {
                    //System.out.println("Error! Running process " + runningProcess + " is not holding resource " + r + " (-1)");
                    output = " -1";
                } else if (k <= 0) {
                    //System.out.println("Error! Please release a nonzero number of units from a resource (-1)");
                    output = " -1";
                } else if (PCB[runningProcess].resources.containsKey(r) && PCB[runningProcess].resources.get(r) < k) {
                    //System.out.println("Error! Too many resources to release (-1)");
                    output = " -1";
                } else {
                    release(r, k);
                    output = " " + String.valueOf(runningProcess);
                }
            }

            // timeout command
            case "to" -> {
                timeout();
                output = " " + String.valueOf(runningProcess);
            }
            default -> {
                //System.out.println("Invalid Command!");
                output = " -1";
            }
        }
        return output;
    }

    void init() {
        runningProcess = 0;
        activeProcesses = 1;
        PCB = new Process[16];
        RCB = new Resource[4];
        RL = new LinkedList[3];
        PCB[0] = new Process(0);

        // create Resources
        RCB[0] = new Resource(1);
        RCB[1] = new Resource(1);
        RCB[2] = new Resource(2);
        RCB[3] = new Resource(3);

        // create ready list
        for (int k = 0; k < 3; k++) {
            RL[k] = new LinkedList();
        }

        // create process 0
        RL[0].addFirst(PCB[0]);
    }

    void create(int p) {
        int PCBIndex = 0;
        
        // look for free spot in PCB
        while (PCB[PCBIndex] != null) {
            PCBIndex++;
        }
        PCB[PCBIndex] = new Process(PCBIndex);
        PCB[PCBIndex].setStateReady();
        PCB[PCBIndex].priority = p;
        PCB[runningProcess].addChild(PCBIndex);
        PCB[PCBIndex].setParent(runningProcess);
        RL[p].addLast(PCB[PCBIndex]);
        
        addDescendant(runningProcess, PCBIndex);
        
        //System.out.println("process " + PCBIndex + " created");

        // check priority
        if (p > PCB[runningProcess].priority) {
            scheduler();
        }
        activeProcesses++;
    }
    
    // helper function for create()
    void addDescendant(int p, int i)
    {
        if (PCB[p].getParent() != 99)
        {
            int parent = PCB[p].getParent();
            addDescendant(parent, i);
        }
        PCB[p].descendants.addLast(i);
    }

    void destroy(int j) {
        //int contextSwitch = 0;
        while (!PCB[j].children.isEmpty()) {
            int child = PCB[j].children.pollFirst();
            destroy(child);
        }

        // remove process j from ready list
        int priority = PCB[j].priority;
        RL[priority].remove(PCB[j]);

        // check if process j is blocked
        if (PCB[j].isBlocked()) {
            int r = PCB[j].waitingOnResource;
            removeResource(j, r);
            PCB[j].setStateReady();

            /*
            if (PCB[j].priority > PCB[runningProcess].priority)
            {
                contextSwitch = 1;
            }
             */
        }

        // release the resources
        while (!PCB[j].resources.isEmpty()) {
            int oldRunningProcess = runningProcess;
            runningProcess = j;

            // check if j is blocked
            /*
            if (PCB[j].isBlocked())
            {
                System.out.println("is this part necessary?");
                int r = PCB[j].waitingOnResource;
                unblock(j, r);
                PCB[j].setStateReady();
                if (PCB[j].priority > PCB[runningProcess].priority)
                {
                    contextSwitch = 1;
                }
            }
             */
            int resource = PCB[j].heldResources.getFirst();
            int units = PCB[j].resources.get(resource);
            release(resource, units);
            runningProcess = oldRunningProcess;
        }

        int parent = PCB[j].getParent();
        PCB[parent].children.removeFirstOccurrence(j);
        
        removeDescendant(parent, j);
        
        PCB[j] = null;
        activeProcesses--;

        /*
        if (j == runningProcess || contextSwitch == 1)
        {
            scheduler();
        }
         */
        scheduler();
    }
    
    // helper function for destroy()
    void removeDescendant(int p, int i)
    {
        if (PCB[p].getParent() != 99)
        {
            int parent = PCB[p].getParent();
            removeDescendant(parent, i);
        }
        PCB[p].descendants.removeFirstOccurrence(i);
    }

    // helper function for delete()
    void removeResource(int p, int r) {
        for (int i = 0; i < RCB[r].waitlist.size(); i++) {
            if (RCB[r].waitlist.get(i).first == p) {
                RCB[r].waitlist.remove(i);
                break;
            }
        }
    }

    void request(int r, int k) {
        if (RCB[r].state >= k) {
            RCB[r].state -= k;
            PCB[runningProcess].addUnits(r, k);
        } else {
            
            //System.out.println("Process " + runningProcess + " blocked");
            WaitlistPair wlp = new WaitlistPair(runningProcess, k);
            int priority = PCB[runningProcess].priority;
            PCB[runningProcess].setStateBlocked(r);
            RL[priority].removeFirstOccurrence(PCB[runningProcess]);
            RCB[r].waitlist.addLast(wlp);
            scheduler();
        }
    }

    void release(int r, int k) {
        PCB[runningProcess].removeUnits(r, k);
        RCB[r].state += k;

        var waitlist = RCB[r].waitlist.iterator();

        // check waitlist for blocked processes
        while (!RCB[r].waitlist.isEmpty() && RCB[r].state > 0) {
            WaitlistPair wlp = waitlist.next();
            int process = wlp.first;
            int requestedUnits = wlp.second;

            if (RCB[r].state >= requestedUnits) {
                RCB[r].state -= requestedUnits;
                PCB[process].addUnits(r, requestedUnits);
                PCB[process].setStateReady();
                waitlist.remove();
                int priority = PCB[process].priority;
                RL[priority].addLast(PCB[process]);
            } else {
                break;
            }
        }
        scheduler();
    }

    void timeout() {
        //System.out.println("timeout called on process " + runningProcess);
        int priority = PCB[runningProcess].priority;
        Process p = RL[priority].pollFirst();

        RL[priority].addLast(p);
        scheduler();
    }

    void scheduler() {
        for (LinkedList<Process> i : RL) {
            if (!i.isEmpty()) {
                runningProcess = i.getFirst().getID();
            }
        }
        //System.out.println("process " + runningProcess + " running");    
    }
}

public class CS143BProject1 {

    public static void main(String[] args) {
        Manager manager = new Manager();
        manager.run();
    }
}
