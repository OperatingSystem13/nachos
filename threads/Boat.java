package nachos.threads;
import nachos.ag.BoatGrader;
import nachos.machine.Lib;

public class Boat
{
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
//	System.out.println("\n ***Testing Boats with only 2 children***");
//	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
  	begin(5, 5, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	
	//
	island = new Lock();
	//m_island = new Lock();
	boat = new Lock();
	//o_adult = new Condition(o_island);
	//o_child = new Condition(o_island);
	//m_child = new Condition(m_island);
	o_adult = new Condition(island);
	o_child = new Condition(island);
	m_child = new Condition(island);
	num_adult_o = adults;
	num_child_o = children;
	num_child_m = 0;
	boat_position = 0;
	boat_num = 0;
	ready_to_finish = 0;
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	/*Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();

            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();*/
	
	//
	KThread[] threads = new KThread[children + adults];
	for(int i = 0; i < children; i++){
		Runnable r = new Runnable() {
		    public void run() {
	                ChildItinerary();
	            }
	        };
	        threads[i] = new KThread(r);
	        threads[i].setName("Child Thread");
	        threads[i].fork();
	}
	for(int i = 0; i < adults; i++){
		Runnable r = new Runnable() {
		    public void run() {
		    		AdultItinerary();
	            }
	        };
	        threads[children + i] = new KThread(r);
	        threads[children + i].setName("Adult Thread");
	        threads[children + i].fork();
	}
	
	//island.acquire();
	//o_child.wake();
	//while (true) KThread.yield();
	//o_island.release();
	
	 for(int i = 0; i < children + adults; i++){
    	 threads[i].join();
     }
    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
	
	//
	island.acquire();
	o_adult.sleep();
	Lib.assertTrue(boat_position == 0);
	boat.acquire();
	num_adult_o = num_adult_o - 1;
	//island.release();
	bg.AdultRowToMolokai();
	boat_position = 1;
	boat.release();
	//island.acquire();
	m_child.wake();
	island.release();
	//
	KThread.finish();
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
	
	//
	island.acquire();
	//o_child.sleep();
	while(num_adult_o + num_child_o > 1){	
		boat.acquire();
		while(boat_position != 0) {
				boat.release();
				o_child.sleep();
				//System.out.println("WAKE");
				boat.acquire();				
		}
		Lib.assertTrue(boat_position == 0);
		num_child_o = num_child_o - 1;
		if(boat_num == 0){
			bg.ChildRowToMolokai();
			o_child.wake();
			boat_num = 1;
			//island.acquire();
			boat.release();
			//island.release();
			num_child_m = num_child_m + 1;
			//m_island.release();
			m_child.sleep();
		}
		else{
			bg.ChildRideToMolokai();
			//island.release();
			boat_position = 1;
			boat_num = 0;
			//island.acquire();	
			boat.release();
			num_child_m = num_child_m + 1;
			//KThread.yield();
			m_child.wake();
			m_child.sleep();
		}
		if(ready_to_finish == 1){
			m_child.wake();
			island.release();
			KThread.finish();
		}
		boat.acquire();
			//System.out.println("HERE");
		Lib.assertTrue(boat_position == 1);
		num_child_m = num_child_m - 1;
		//island.release();
		bg.ChildRowToOahu();
		boat_position = 0;
		boat.release();
		//island.acquire();
		num_child_o = num_child_o + 1;
		//if(num_adult_o != 0) o_adult.wake();
		if(num_child_o < 2) o_adult.wake();
		else o_child.wake();
		o_child.sleep();
		continue;
	}
	boat.acquire();
	Lib.assertTrue(boat_position == 0);
	num_child_o = num_child_o - 1;
	Lib.assertTrue(num_child_o + num_adult_o == 0);
	if(boat_num == 0) { 
		bg.ChildRowToMolokai();
	}
	else {
		bg.ChildRideToMolokai();
	}
	boat_num = 0;
	boat_position = 1;
	boat.release();
	//island.release();
	//island.acquire();
	num_child_m = num_child_m + 1;
	ready_to_finish = 1;
	//m_child.sleep();
	m_child.wake();
	island.release();
	KThread.finish();
	//
	
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
    static Lock island;
    //static Lock m_island;
    static Lock boat;
    static Condition o_adult;
    static Condition o_child;
    static Condition m_child;
    static int num_adult_o;
    static int num_child_o;
    static int num_child_m;
    static  int boat_position;
    static  int boat_num;
    static int ready_to_finish;
}
