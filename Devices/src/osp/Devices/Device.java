package osp.Devices;

/**
    This class stores all pertinent information about a device in
    the device table.  This class should be sub-classed by all
    device classes, such as the Disk class.

    @OSPProject Devices
*/

import osp.IFLModules.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Tasks.*;
import java.util.*;
import java.lang.Math;


public class Device extends IflDevice
{
	
	static GenericList iorbQueue;

    /**
        This constructor initializes a device with the provided parameters.
	As a first statement it must have the following:

	    super(id,numberOfBlocks);

	@param numberOfBlocks -- number of blocks on device

        @OSPProject Devices
    */
    public Device(int id, int numberOfBlocks)
    {
       
    	super(id,numberOfBlocks);

    }

    /**
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Devices
    */
    public static void init()
    {
    	iorbQueue = new GenericList();


    }

    /**
       Enqueues the IORB to the IORB queue for this device
       according to some kind of scheduling algorithm.
       
       This method must lock the page (which may trigger a page fault),
       check the device's state and call startIO() if the 
       device is idle, otherwise append the IORB to the IORB queue.

       @return SUCCESS or FAILURE.
       FAILURE is returned if the IORB wasn't enqueued 
       (for instance, locking the page fails or thread is killed).
       SUCCESS is returned if the IORB is fine and either the page was 
       valid and device started on the IORB immediately or the IORB
       was successfully enqueued (possibly after causing pagefault pagefault)
       
       @OSPProject Devices
    */
    public int do_enqueueIORB(IORB iorb)
    {
    	MyOut.print(iorb, " >>>>>>>> enqueue");
    	/*block size*/
    	int blockSize = (int) (java.lang.Math.pow(2,(MMU.getVirtualAddressBits() - MMU.getPageAddressBits())));

    	//Device.get(iorb.getDeviceID())
    	
    	/*Numero de setores por bloco*/
    	int numSectorBlock = blockSize/((Disk) this).getBytesPerSector();
    	
    	/*numero de blocos na track*/
    	int numBlockTrack = ((Disk) this).getSectorsPerTrack()/numSectorBlock;
    	
    	
    	/*Cilindro é o bloco dado pelo numero de blocos por track pelo numero de cilindros*/
    	int cylinder = (iorb.getBlockNumber()/numBlockTrack)/((Disk) this).getPlatters();
    	
    	
    	
    	iorb.getPage().lock(iorb);/*lock the page associated with the iorb*/
    	
    	/*increment the IORB count of the open-file handle associated with iorb.*/
    	iorb.getOpenFile().incrementIORBCount();
    	
    	iorb.setCylinder(cylinder);/*Not sure*/
    	
    	if (iorb.getThread().getStatus() == ThreadKill){
    		return FAILURE;
    	}
    	
    	/*Se não estiver ocupado inicializa o IO*/
    	if(!(Device.get(iorb.getDeviceID()).isBusy())){
    		startIO(iorb);
    		return SUCCESS;
    	}
    		
    	else{
    	//if((Device.get(iorb.getDeviceID()).isBusy())){    	   	
    		iorbQueue.append(iorb);
    		return SUCCESS;
    	//}
    	}
    }

    /**
       Selects an IORB (according to some scheduling strategy)
       and dequeues it from the IORB queue.

       @OSPProject Devices
    */
    public IORB do_dequeueIORB()
    {
    	MyOut.print("iorb", " >>>>>>>> dequeue");	
			return  (IORB) iorbQueue.removeHead();		
	

    }

    /**
        Remove all IORBs that belong to the given ThreadCB from 
	this device's IORB queue

        The method is called when the thread dies and the I/O 
        operations it requested are no longer necessary. The memory 
        page used by the IORB must be unlocked and the IORB count for 
	the IORB's file must be decremented.

	@param thread thread whose I/O is being canceled

        @OSPProject Devices
    */
    public void do_cancelPendingIO(ThreadCB thread)
    {
    	IORB iorb;
    	MyOut.print(thread, " >>>>>>>> cancel");	
    	while(iorbQueue.forwardIterator().hasMoreElements()){
    		
    		 iorb = ((IORB) iorbQueue.forwardIterator().nextElement());
    		 
    		if (iorb.getThread().getID() == thread.getID()){
    			/*liberar o buffer*/
    			iorb.getPage().unlock();
    			/*Decrementar o contador*/
    			iorb.getOpenFile().decrementIORBCount();
    			
    			/*Se a flag é verdadeira e o contado é zero, deve-se fechar algum arquivo pendente*/
    			if ((iorb.getOpenFile().closePending) &&
    			(iorb.getOpenFile().getIORBCount() == 0))
    			{
    				iorb.getOpenFile().close();
    				
    			}
    		}
    	}  	

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
	
	@OSPProject Devices
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
