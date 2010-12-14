package osp.Devices;
import java.util.*;
import osp.IFLModules.*;
import osp.Hardware.*;
import osp.Interrupts.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Tasks.*;
import osp.Memory.*;
import osp.FileSys.*;

/**
    The disk interrupt handler.  When a disk I/O interrupt occurs,
    this class is called upon the handle the interrupt.

    @OSPProject Devices
*/
public class DiskInterruptHandler extends IflDiskInterruptHandler
{
    /** 
        Handles disk interrupts. 
        
        This method obtains the interrupt parameters from the 
        interrupt vector. The parameters are IORB that caused the 
        interrupt: (IORB)InterruptVector.getEvent(), 
        and thread that initiated the I/O operation: 
        InterruptVector.getThread().
        The IORB object contains references to the memory page 
        and open file object that participated in the I/O.
        
        The method must unlock the page, set its IORB field to null,
        and decrement the file's IORB count.
        
        The method must set the frame as dirty if it was memory write 
        (but not, if it was a swap-in, check whether the device was 
        SwapDevice)

        As the last thing, all threads that were waiting for this 
        event to finish, must be resumed.

        @OSPProject Devices 
    */
    public void do_handleInterrupt()
    {
    	MyOut.print("handle", " >>>>>>>> handle");
    	/* Evento iorb que gerou a interrupcao*/
    	IORB iorb = (IORB)InterruptVector.getEvent();
    	/*Decrementando contador*/
    	iorb.getOpenFile().decrementIORBCount();
    	
    	/*Caso o arquivo não ira mais ser utilizado*/
		if ((iorb.getOpenFile().closePending) &&
    			(iorb.getOpenFile().getIORBCount() == 0))
    	{
    				iorb.getOpenFile().close();
    				
    	}

		/*liberando a pagina*/
		iorb.getPage().unlock();
		
		/*Caso a Task esta viva*/
		if(iorb.getThread().getTask().getStatus() == TaskLive){
			/*E nao esta realizando swap-in e nem swap-out*/
			if(iorb.getDeviceID() != SwapDeviceID){
				/*marcar referencia*/
				iorb.getPage().getFrame().setReferenced(true);
				/*se leitura marcar como dirty*/
				if(iorb.getIOType()==FileRead){
					iorb.getPage().getFrame().setDirty(true);
				}
			}
			/*Se esta realizando swap marcar dirty como falso*/
			else{
				iorb.getPage().getFrame().setDirty(false);
			}
		}
		
		/*Caso o frame reservado seja igual a task*/
		if(iorb.getThread().getTask().getStatus() == TaskTerm){
			if(iorb.getPage().getFrame().getReserved().getID() == iorb.getThread().getTask().getID()){
				/*marcar como não reservada a task*/
				iorb.getPage().getFrame().setUnreserved(iorb.getThread().getTask());
			}
			
		}
		
		/*notificar as Threads*/
		iorb.notifyThreads();
		
		/*liberar device*/
		Device.get(iorb.getDeviceID()).setBusy(false);
		
		/*retirar um iorb da fila*/
		IORB dequeue = Device.get(iorb.getDeviceID()).dequeueIORB();
		
		/*Se nao for nulo iniciar outro I/O*/
		if(dequeue != null){
			Device.get(iorb.getDeviceID()).startIO(dequeue);			
		}
		
		ThreadCB.dispatch();		
		
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
