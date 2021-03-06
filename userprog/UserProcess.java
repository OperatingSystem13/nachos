package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*;
import java.awt.Point;
import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
	  /* 
     * Remove for 2.2. pageTable allocation move to loadSection
     * 
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
	*/
	 public UserProcess() {
	//added for 2.1
	files = new OpenFile[16];
	files[0] = UserKernel.console.openForReading();
	files[1] = UserKernel.console.openForWriting();
	//
	
	//added for 2.3
	boolean intStatus = Machine.interrupt().disable();
	this.ID = IDcount;
	IDcount++;
	pcount++;
	Machine.interrupt().restore(intStatus);
	children = new LinkedList<Point>();
	child = new LinkedList<UserProcess>();
	//
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	thread = new UThread(this);
	thread.setName(name);
	thread.fork();
	
	//new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	
	//removed for 2.2
	// for now, just assume that virtual addresses equal physical addresses
	/*if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);*/
	//
	
	//added for 2.2
	int amount = 0;
	int nextaddr;
	for (int addr = vaddr; amount < length; addr = nextaddr) {
		nextaddr = (vaddr / Processor.pageSize + 1) * Processor.pageSize;
		
		int ppn = pageTable[Processor.pageFromAddress(addr)].ppn;
		int poffset = Processor.offsetFromAddress(addr);
		int paddr = Processor.makeAddress(ppn, poffset);
		if (paddr < 0 || paddr >= memory.length)
		    return amount;
		int thisLen = Math.min(Math.min(nextaddr - addr, length - amount), memory.length-addr);
		
		System.arraycopy(memory, paddr, data, offset + amount, thisLen);
		amount += thisLen;
	}
	//
	
	return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	//removed for 2.2
	// for now, just assume that virtual addresses equal physical addresses
	/*if (vaddr < 0 || vaddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(data, offset, memory, vaddr, amount);*/
	//
	
	//added for 2.2
	int amount = 0;
	int nextaddr;
	for (int addr = vaddr; amount < length; addr = nextaddr) {
		nextaddr = (vaddr / Processor.pageSize + 1) * Processor.pageSize;
		if (pageTable[Processor.pageFromAddress(addr)].readOnly)
			continue;
		
		int ppn = pageTable[Processor.pageFromAddress(addr)].ppn;
		int poffset = Processor.offsetFromAddress(addr);
		int paddr = Processor.makeAddress(ppn, poffset);
		if (paddr < 0 || paddr >= memory.length)
		    return amount;
		int thisLen = Math.min(Math.min(nextaddr - addr, length - amount), memory.length-addr);
		
		System.arraycopy(data, offset + amount, memory, paddr, thisLen);
		amount += thisLen;
	}
	//
	
	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
    //removed for 2.2
	/*if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}*/
   //

    	
    //added for 2.2
    	pageTable = new TranslationEntry[numPages];
        for (int vpn = 0; vpn < numPages; vpn++) {
        	int ppn = UserKernel.allocatePage();
    		if (ppn == -1) {
    		    coff.close();
    		    Lib.debug(dbgProcess, "\tinsufficient physical memory");
    		    return false;
    		}
    		pageTable[vpn] = new TranslationEntry(vpn,ppn, true,false,false,false);
        }
    //
    	
	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		// for now, just assume virtual addresses=physical addresses
		section.loadPage(i, vpn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    //added for 2.2
    	for (int vpn = 0; vpn < numPages; vpn++) {
        	Lib.assertTrue( UserKernel.releasePage(pageTable[vpn].ppn));
        }
    //
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

    //added for 2.1
    if(this.ID != 0) return 0;
    //
    	
	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

    //added for 2.1

    private int handleCreate(int address){
    	int result = -1;
    	String name = readVirtualMemoryString(address, 256);
    	if(name == null) return -1;
    	boolean flag = false;
    	for(int i = 2; i < 16; i++){
    		if(files[i] == null) {
    			flag = true;
    			continue;
    		}
    		if(files[i].getName().equals(name)){
    			return i;
    		}
    	}
    	if(flag == false) return -1;
    	boolean create = true;
    	OpenFile re = ThreadedKernel.fileSystem.open(name, create);
    	for(int i = 2; i < 16; i++){
    		if(files[i] == null){
    			result = i;
    			files[i] = re;
    			break;
    		}	
    	}
    	return result;
    }
    
    private int handleOpen(int address){
    	int result = -1;
    	String name = readVirtualMemoryString(address, 256);
    	if(name == null) return -1;
    	boolean flag = false;
    	for(int i = 2; i < 16; i++){
    		if(files[i] == null) {
    			flag = true;
    			continue;
    		}
    		if(files[i].getName().equals(name)){
    			return i;
    		}
    	}
    	if(flag == false) return -1;
    	boolean create = false;
    	OpenFile re = ThreadedKernel.fileSystem.open(name, create);
    	for(int i = 2; i < 16; i++){
    		if(files[i] == null){
    			result = i;
    			files[i] = re;
    			break;
    		}	
    	}
    	return result;
    }
    
    private int handleRead(int index, int address, int count){
    	int result = -1;
    	if(index < 0 || index > 15 || index == 1) return -1;
    	if(count < 0) return -1;
    	if(files[index] == null) return -1;
    	byte[] buffer = new byte[count];
    	result = files[index].read(buffer, 0, count);
    	if(result < 0) return 0;
    	writeVirtualMemory(address, buffer);
    	return result;
    }
    
    private int handleWrite(int index, int address, int count){
    	int result = -1;
    	if(index <= 0 || index > 15) return -1;
    	if(count < 0) return -1;
    	if(files[index] == null) return -1;
    	byte[] buffer = new byte[count];
    	result = readVirtualMemory(address, buffer, 0, count);
    	if(result < 0) return 0;
    	result = files[index].write(buffer, 0, result);
    	if(result < count) return -1;
    	else return result;
    }
    
    private int handleClose(int index){
    	int result = -1;
    	if(index < 2 || index > 15) return -1;
    	if(files[index] == null) return -1;
    	else {
    		files[index].close();
    		files[index] = null;
    		return 0;
    	}
    }
    
    private int handleUnlink(int address){
    	int result = -1;
    	String name = readVirtualMemoryString(address, 256);
    	if(name == null) return -1;
    	for(int i = 2; i < 16; i++){
    		if(files[i] == null) continue;
    		if(files[i].getName().equals(name)){
    			files[i] = null;	
    		}
    	}
    	ThreadedKernel.fileSystem.remove(name);
		return 0;
    }
    //
    
    //added for 2.3
    private int handleExit(int status){
    	coff.close();
    	for(int i = 2; i < 16; i++){
    		if(files[i] != null){
    			files[i].close();
    			files[i] = null;
    		}
    	}
    	unloadSections();
    	pcount--;
    	if(father != null){
    		for(int i = 0; i < father.children.size(); i++){
    			if(father.children.get(i).getX() == this.ID){
    				father.children.get(i).setLocation(this.ID, status);
    			}
    		}
    		for(int i = 0; i < father.child.size(); i++){
    			if(father.child.get(i).ID == this.ID){
    				father.children.remove(i);
    			}
    		}
    	}
    	if(pcount == 0)  Kernel.kernel.terminate();
    	return 0;
    }
    
    private int handleExec(int faddress, int argc, int argv){
    	if(faddress < 0) return -1;
    	String name = readVirtualMemoryString(faddress, 256);
    	if(name == null) return -1;
    	if(argc < 0) return -1;
    	if(argv < 0) return -1;
    	String args[] = new String[argc];
    	for(int i = 0; i < argc; i++){
    		byte[] temp = new byte[4];
    		readVirtualMemory(argv + i * 4, temp);
    		int result = Lib.bytesToInt(temp, 0);
    		if(result < 0) return -1;
    		args[i] = readVirtualMemoryString(result, 256);
    	}
    	UserProcess newprocess = new UserProcess();
    	newprocess.father = this;
    	this.children.add(new Point(newprocess.ID, 0));
    	this.child.add(newprocess);
    	boolean res = newprocess.execute(name, args);
    	if(res == false) return -1;
    	return newprocess.ID;
    }
    
    private int handleJoin(int pid, int address){
    	if(address < 0) return -1;
    	int result = 0;
    	boolean flag = false;
    	int index = 0;
    	int index2 = 0;
    	for(int i = 0; i < children.size(); i++){
    		if(children.get(i).getX() == pid){
    			index = i;
    			flag = true;
    		}
    	}
    	if(flag == false) return -1;
    	for(int i = 0; i < child.size(); i++){
    		if(child.get(i).ID == pid){
    			index2 = i;
    		}
    	}
    	child.get(index2).thread.join();
    	result = (int)(children.get(index).getY());
    	byte[] temp = new byte[4];
    	temp = Lib.bytesFromInt(result);
    	writeVirtualMemory(address, temp);
    	return 0;
    }
    //
    
    private static final int
    syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	    
	//added for 2.1
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0, a1, a2);
	case syscallWrite:
		return handleWrite(a0,a1, a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);
	//
		
	//added for 2.3
	case syscallExit:
		return handleExit(a0);
	case syscallExec:
		return handleExec(a0, a1, a2);
	case syscallJoin:
		return handleJoin(a0, a1);
	//

	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    
    //added for 2.1
    private OpenFile[] files;
    //
    
    //added for 2.3
    public int ID;
    private static int IDcount = 0;
    private static int pcount = 0;
    private UserProcess father = null;
    private LinkedList<Point> children;
    private LinkedList<UserProcess> child;
    private KThread thread;
    //
}
