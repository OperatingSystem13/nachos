package nachos.threads;

import nachos.machine.*;
import java.util.Random;


/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
			status=NEW;
			conditionLock=new Lock();
			lockForChannel=new Condition( conditionLock );
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
			conditionLock.acquire();
			while(status == READ || status == READY) {
					lockForChannel.wake();
					lockForChannel.sleep();
			}
			if(status == NEW) {
					this.word = word;
					status = READ;
					lockForChannel.wake();
					lockForChannel.sleep();
			}
			else {
					this.word = word;
					lockForChannel.wake();
			}
			conditionLock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
			int rword;
			conditionLock.acquire();
			while(status == WRITE || status == READY) {
					lockForChannel.wake();
					lockForChannel.sleep();
			}
			if(status == NEW) {
					status = WRITE;
					lockForChannel.wake();
					lockForChannel.sleep();
					rword = this.word;
					status = NEW;
			}
			else {
					status = NEW;
					rword = this.word;
					lockForChannel.wake();
			}
			conditionLock.release();

	return rword;
    }
    
    private static class TestClass implements Runnable{
    	TestClass(Communicator channel, int[] data){
    		this.channel = channel;
    		this.data = data;
    	}
    	
    	public void run(){
    	for(int i = 0; i < 4; i++){
    			channel.speak(data[i]);
    			System.out.println("speak: " + data[i]);
    			
    		}
    	}
    	private Communicator channel;
    	private int[] data;
    }
    
    public static void selfTest(){
    	int[] data = new int[4];
    	int[] received = new int[4];
    	for(int i = 0; i < 4; i++){
            Random random = new Random();
            data[i] = random.nextInt() % 50;
    	}
    	Communicator test = new Communicator();
    	new KThread(new TestClass(test, data)).setName("sender").fork();
    	for(int i = 0; i < 4; i++){
			received[i] = test.listen();
			System.out.println("listen: " + received[i]);
			
		}
    	
    	/*for(int i= 0; i < 4; i++){
    		Lib.assertTrue(data[i] == received[i]);
    	}*/
    }

	private Condition lockForChannel;
	private int status;
	private static int NEW=0;
	private static int READ=1;
	private static int WRITE=2;
	private static int READY=3;
	private int word;
	private Lock conditionLock;
}
