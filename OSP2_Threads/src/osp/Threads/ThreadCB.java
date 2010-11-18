/*
* Grupo 26
* RA: 092549 - Paolo Barreto Nunes da Silva 
* RA: 097107 - Wellington da Silva Gomes
*
* Status: Final
*
* 30/09/2010
* 1. Alteração 1 - Estamos estudando o projeto
* 2. Alteração 2 - Estamos estudando o projeto, fazendo as devidas leituras do OSP2
* 3. Alteração 3 - Finalização do projeto
*/

package osp.Threads;

import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
* This class is responsible for actions related to threads, including creating,
* killing, dispatching, resuming, and suspending threads.
*/
public class ThreadCB extends IflThreadCB {
	
	static GenericList readyQueue;
	
	public ThreadCB() {
		super();
	}
	
	/**
	* This method will be called once at the beginning of the simulation. The
	* student can set up static variables here.
	*/
	public static void init() {
		readyQueue = new GenericList();
	}
	
	/**
	* Sets up a new thread and adds it to the given task. 
	* The method must set the ready status and attempt to add thread to task.
	* If the latter fails because there are already too many threads in this
	* task, so does this method, otherwise, the thread is appended to the ready
	* queue and dispatch() is called.
	* The priority of the thread can be set using the getPriority/setPriority
	* methods. However, OSP itself doesn't care what the actual value of the
	* priority is. These methods are just provided in case priority scheduling
	* is required.
	* @return thread or null
	*/
    static public ThreadCB do_create(TaskCB task) {
    	
    	// Criacao do objeto thread
		ThreadCB newThread = new ThreadCB();
        
		// Verificar se pode adicionar a Thread na Task
		if (task.getThreadCount() >= ThreadCB.MaxThreadsPerTask)
        	return null;
		
		// Adicionar na task
		if (task.addThread(newThread) == FAILURE)
			return null;
		
		newThread.setTask(task); // Thread pertence a Task
		newThread.setPriority(0); // Sem prioridade no projeto
		newThread.setStatus(ThreadReady);	
		readyQueue.append(newThread); // OK, na ready queue
		
		dispatch();
		return newThread;
	}
	
	/** 
	* Kills the specified thread. 
	* The status must be set to ThreadKill, the thread must be removed from the
	* task's list of threads and its pending IORBs must be purged from all
	* device queues.
	* If some thread was on the ready queue, it must removed, if the thread was
	* running, the processor becomes idle, and dispatch() must be called to
	* resume a waiting thread.
	*/
	public void do_kill() {
		
		TaskCB task = this.getTask();
		
		switch(this.getStatus()) {
			case ThreadKill:
				break;
			case ThreadReady:
				readyQueue.remove(this);
				break;
			case ThreadRunning:
				MMU.getPTBR().getTask().setCurrentThread(null);
				MMU.setPTBR(null);
				break;
			default:
				for(int i=0; i < Device.getTableSize(); i++)
					Device.get(i).cancelPendingIO(this);
				break;
		}
		
		this.setStatus(ThreadKill);
		task.removeThread(this);
		ResourceCB.giveupResources(this);
		if(task.getThreadCount() == 0)
			task.kill();
		ThreadCB.dispatch();
	}

	/**
	* Suspends the thread that is currenly on the processor on the specified
	* event. 
	* Note that the thread being suspended doesn't need to be running. It can
	* also be waiting for completion of a pagefault and be suspended on the
	* IORB that is bringing the page in.
	* Thread's status must be changed to ThreadWaiting or higher, the processor
	* set to idle, the thread must be in the right waiting queue, and
	* dispatch() must be called to give CPU control to some other thread.
	* @param event - event on which to suspend this thread.
	*/
	public void do_suspend(Event event) {
		
		switch(this.getStatus()) {
			case ThreadKill:
				break;
			case ThreadReady:
				return;
			case ThreadRunning:
				event.addThread(MMU.getPTBR().getTask().getCurrentThread());
				MMU.getPTBR().getTask().getCurrentThread().setStatus(ThreadWaiting);
				MMU.getPTBR().getTask().setCurrentThread(null);
				MMU.setPTBR(null);
				break;
			default:
				event.removeThread(this);
				this.setStatus(this.getStatus()+1);
				event.addThread(this);
				break;
		}
		ThreadCB.dispatch();
	}
	
	/**
	* Resumes the thread.
	* Only a thread with the status ThreadWaiting or higher can be resumed. The
	* status must be set to ThreadReady or decremented, respectively.
	* A ready thread should be placed on the ready queue.
	*/
	public void do_resume() {
		
		switch(this.getStatus()) {
			case ThreadKill:
				break;
			case ThreadReady:
				return;
			case ThreadWaiting:
				this.setStatus(ThreadReady);
				readyQueue.append(this);
				break;
			default:
				this.setStatus(this.getStatus()-1);
				break;
		}
		ThreadCB.dispatch();
	}

	/**
	* Selects a thread from the run queue and dispatches it. 
	* If there is just one theread ready to run, reschedule the thread
	* currently on the processor.
	* In addition to setting the correct thread status it must update the PTBR.
	* @return SUCCESS or FAILURE
	*/
	public static int do_dispatch() {
		
		// Pega primeira thread da readyQueue
		ThreadCB thread = (ThreadCB) readyQueue.removeHead();
		
		if(MMU.getPTBR() != null && thread != null) {
			// Thread rodando, preemptivo
			readyQueue.append(MMU.getPTBR().getTask().getCurrentThread());
			MMU.getPTBR().getTask().getCurrentThread().setStatus(ThreadReady);
			MMU.getPTBR().getTask().setCurrentThread(null);
			MMU.setPTBR(null);
			thread.setStatus(ThreadRunning);
			MMU.setPTBR(thread.getTask().getPageTable());
			thread.getTask().setCurrentThread(thread);
		}
		else if(MMU.getPTBR() != null && thread == null) {
			// Thread continua na CPU
			return SUCCESS;
		}
		else if(MMU.getPTBR() == null && thread != null) {
			// Coloca thread na CPU
			thread.setStatus(ThreadRunning);
			MMU.setPTBR(thread.getTask().getPageTable());
			thread.getTask().setCurrentThread(thread);
		}
		else {
			return FAILURE;
		}
		
		return FAILURE;
	}
	
	/**
	* Called by OSP after printing an error message. The student can insert
	* code here to print various tables and data structures in their state just
	* after the error happened.  The body can be left empty, if this feature is
	* not used.
	*/
	public static void atError() {
		// No code
	}
	
	/**
	* Called by OSP after printing a warning message. The student can insert
	* code here to print various tables and data structures in their state just
	* after the warning happened.
	* The body can be left empty, if this feature is not used.
	*/
	public static void atWarning() {
		// No code
	}
	
	/*
	* Feel free to add methods/fields to improve the readability of your code
	*/
}

/*
* Feel free to add local classes to improve the readability of your code
*/
