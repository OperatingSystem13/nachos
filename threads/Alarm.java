package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
/***/
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
		waitQueue=new ArrayList<waiting>();
		queueControl = new Semaphore(1);
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
	KThread.currentThread().yield();
		int i;
		long currentTime = Machine.timer().getTime();
		queueControl.P();		
		for(i = 0; i < waitQueue.size(); i++) {
				waiting next = (waiting)waitQueue.get(i);
				if(currentTime >= next.wakeTime) {			
						(next.item).ready();
						waitQueue.remove(i);
						i--;
				}
		}
		queueControl.V();
		
		
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	long wakeTime = Machine.timer().getTime() + x;
	boolean intStatus = Machine.interrupt().disable();	
	queueControl.P();
	waiting newItem = new waiting();
	newItem.item = KThread.currentThread();
	newItem.wakeTime = wakeTime;
	waitQueue.add(newItem);
	queueControl.V();	
	KThread.sleep();
	Machine.interrupt().restore(intStatus);
	//while (wakeTime > Machine.timer().getTime())
	//    KThread.yield();
    }
	public class waiting {
		public KThread item;
		public long wakeTime;
	}
	ArrayList<waiting> waitQueue;
	Semaphore queueControl;
}
