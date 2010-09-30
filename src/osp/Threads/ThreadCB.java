/*
* Grupo 26
* RA: 092549
* RA: 097107
*
* Status:Em andamento
*
* 23/09/2010
* 1. Alteração 1 - Estamos estudando o projeto
* 2. Alteração 2 - Estamos estudando o projeto, fazendo as devidas leituras do OSP2
* 3. Alteração 3
*
* */ 
package osp.Threads;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Enumeration;

import javax.xml.crypto.dsig.spec.HMACParameterSpec;

import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
	static ArrayList<ThreadCB> readyQueue = new ArrayList<ThreadCB>();
	static ArrayList<ThreadCB> waitingQueue = new ArrayList<ThreadCB>();
	static int t;
    /**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
	
	
    public ThreadCB()
    {
      	super(); //Construtor
    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
        // your code goes here

    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
    	/*Criacao do objeto thread*/
		ThreadCB newThread = new ThreadCB();
        
		/*Verificar se pode adicionar a Thread na Task*/
		if (task.getThreadCount() >= ThreadCB.MaxThreadsPerTask){
        	return null;
        }
		
		/*Adicionar na task*/
		if (task.addThread(newThread) == FAILURE){
			return null;			
		}
		
		newThread.setTask(task);			/*Thread pertence a Task*/
			
		newThread.setPriority(0); 			/*sem prioridade no projeto*/
		
		newThread.setStatus(ThreadReady);	
		
		readyQueue.add(newThread);			/*OK, na ready queue*/

		
		dispatch();
		return newThread;
    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
        // your code goes here

    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {
    	/*Verificar se o status anterior era Running e mudar para Waiting
    	 * Caso o status anterior era waiting, soma mais 1
    	 */
        if (this.getStatus() == ThreadRunning){
        	this.setStatus(ThreadWaiting);        	
        	/*Liberar a Thread da CPU*/
        	MMU.getPTBR().getTask().setCurrentThread(null);        	
        	MMU.setPTBR(null);
        }
        else{
        	this.setStatus(this.getStatus()+1);   	
        }
        
        waitingQueue.add(this);
        readyQueue.remove(this);
        
        /*adicionar a Thread a fila de eventos*/
        event.addThread(this);
        
        

      dispatch();

    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
    		//code
    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    public static int do_dispatch()
    {   
    	int status=0;
    	
    	/*caso readyQueue vazia retornar failure*/
    	if(readyQueue.size() == 0){
    		return FAILURE;
    	}
    	
    	t=(t+1)%readyQueue.size();
    	
    	/*receber status*/
    	if(MMU.getPTBR()!=null){
    		
    		status=MMU.getPTBR().getTask().getCurrentThread().getStatus();
    	
    		/*modificar status*/
    		MMU.getPTBR().getTask().getCurrentThread().setStatus(status);
    	
    		/*modificar a Thread corrente para null*/
    		MMU.getPTBR().getTask().setCurrentThread(null);
    	
    		/*modificar o PTBR para null*/
    		MMU.setPTBR(null);
    	}
    	
    	readyQueue.get(t).setStatus(ThreadRunning);
    	
    	MMU.setPTBR(readyQueue.get(t).getTask().getPageTable());
    	
    	readyQueue.get(t).getTask().setCurrentThread(readyQueue.get(t));
    	
    	readyQueue.remove(t);
    			

    	
    	return SUCCESS;        

    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
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
