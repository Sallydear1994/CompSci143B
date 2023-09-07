// William Barsaloux
// 80697857

package com.mycompany.cs143bproject2;

import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

class VMManager
{
    int [] PM; // Physical Memory
    int [][] D; // Disk
    boolean [] freeFrames;
    
    // for file reading and writing
    File initFile;
    Scanner initData;
    File inputFile;
    Scanner inputData;
    FileWriter outputFile;
    
    VMManager()
    {
        PM = new int[524288];
        D = new int[1024][512];
        freeFrames = new boolean[512];
        
    }
    
    void run()
    {
        
        try
        {
            initializeFreeFrames();
            
            initFile = new File("init.txt");
            initData = new Scanner(initFile);
            
            String STData = initData.nextLine();
            String PTData = initData.nextLine();
            
            initializeST(STData);
            initializePT(PTData);
            
            inputFile = new File("input.txt");
            inputData = new Scanner(inputFile);
            
            String input = inputData.nextLine();
            String output = processInput(input);
            
            outputFile = new FileWriter("output.txt");
            outputFile.write(output);
            
            initData.close();
            inputData.close();
            outputFile.close();
        }
        catch (IOException e)
        {
            System.out.println("error! " + e);
        }
    }
    
    // set all the frames to free state
    void initializeFreeFrames()
    {
        for (int i = 0; i < 512; i++)
        {
            freeFrames[i] = true;
        }
    }
    
    void initializeST(String line)
    {
        String[] STData = line.split(" ");
        int segments = STData.length / 3;
        
        int s;
        int z;
        int f;
        
        // initialize ST
        for (int i = 0; i < segments; i++)
        {
            s = Integer.parseInt(STData[i * 3]);
            z = Integer.parseInt(STData[i * 3 + 1]);
            f = Integer.parseInt(STData[i * 3 + 2]);
            
            PM[2 * s] = z;
            PM[2 * s + 1] = f;
            freeFrames[i] = false;
            
            if (f > 0)
            {
                freeFrames[f] = false;
            }
        }
    }
    
    void initializePT(String line)
    {
        String[] PTData = line.split(" ");
        int pages = PTData.length / 3;
        
        int s;
        int p;
        int f;
        
        // initialize PT
        for (int i = 0; i < pages; i++)
        {
            s = Integer.parseInt(PTData[i * 3]);
            p = Integer.parseInt(PTData[i * 3 + 1]);
            f = Integer.parseInt(PTData[i * 3 + 2]);
            
            if (f > 0)
            {
                freeFrames[f] = false;
            }

            if (PM[2 * s + 1] < 0)
            {
                int b = Math.abs(PM[2 * s + 1]);
                int j = 0;
                while (D[b][j] != 0)
                {
                    j++;
                }
                D[b][j] = f;
            }
            else
            {
                PM[PM[2 * s + 1] * 512 + p] = f;
            }
        }
    }
    
    String processInput(String line)
    {
        String[] data = line.split(" ");
        String result = "";
        
        int s;
        int p;
        int w;
        int pw;
        
        for (int i = 0; i < data.length; i++)
        {
            int binNum = Integer.parseInt(data[i]);
            
            s = binNum >> 18;
            w = binNum & 0x1FF;
            p = (binNum >> 9) & 0x1FF;
            pw = binNum & 0x3FFFF;
            
            // check if PT not present
            if (PM[2 * s + 1] < 0)
            {
                int b = Math.abs(PM[2 * s + 1]);
                int f1 = 0;
                while (!freeFrames[f1])
                {
                    f1++;
                }
                freeFrames[f1] = false;
                read_block(b, f1 * 512);
                PM[2 * s + 1] = f1;
            }
            
            // check if page not resident
            if (PM[PM[s * 2 + 1] * 512 + p] < 0)
            {
                int b = Math.abs(PM[PM[2 * s + 1] * 512 + p]);
                
                int f2 = 0;
                while (!freeFrames[f2])
                {
                    f2++;
                }
                freeFrames[f2] = false;
                read_block(b, f2 * 512);
                PM[PM[2 * s + 1] * 512 + p] = f2;       
            }
            
            // format result
            if (i == 0)
            {
                if (pw < PM[2 * s])
                {
                    result = String.valueOf(PM[PM[2 * s + 1] * 512 + p] * 512 + w);
                }
                else
                {
                    result = "-1";
                }
            }
            else
            {
                if (pw < PM[2 * s])
                {
                    result += " " + String.valueOf(PM[PM[2 * s + 1] * 512 + p] * 512 + w);
                }
                else
                {
                    result += " -1";
                }
            }
        }
        return result;
    }
    
    // copy contents of block into physical memory
    void read_block(int b, int m)
    {
        //for (int i = 0; i < 512; i++)
        //{
        //    PM[m + i] = D[b][i];
        //}
        System.arraycopy(D[b], 0, PM, m, 512);
    }
}

public class CS143BProject2 
{
    public static void main(String[] args) {
        VMManager manager = new VMManager();
        manager.run();
        
    }
}
