import java.util.*;
import java.io.*;
public class Z {
    public static void main(String[] args) {
        File f = new File("zip2fips.json");
        try { 
            Scanner scn = new Scanner(f);
            String contents = scn.useDelimiter("\\Z").next().replaceAll("\\R+", " ");
            System.out.println(contents.charAt(0));
            
            scn = new Scanner(contents);
            String zipName = "75075";
            String fips = "";

            System.out.println(scn.next());
            System.out.println(scn.next());
            System.out.println(scn.next());
            System.out.println(scn.next());
            
            while (scn.hasNext()) {
                String next = scn.next();
                System.out.println(String.valueOf(next));
                if (next.contains(zipName)) {
                    //Stop!
                    fips = scn.next();
                    System.out.println("Fips is: " + fips.split("\"")[1] + " for zip " + zipName);
                    break;
                }
            }
        } catch (Exception e) {} 
        
    }
}
