package osp.Memory;
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {
		if (page.isValid()){			
			return FAILURE;
		}
		
		SystemEvent pfEvent = new SystemEvent(userOption);

		thread.suspend(pfEvent); /*Suspend the thread that caused the interrupt*/

		FrameTableEntry freeFrame = null;
		FrameTableEntry notDirtyFrame = null;
		FrameTableEntry victmFrame = null;

		FrameTableEntry frameofWork = null;
		
		int i=1;
		
		while (i < MMU.getFrameTableSize()){ /* encontrar um frame livre */
				
			FrameTableEntry frame = MMU.getFrame(i);
			
			if (frame.getReserved() == null && frame.getLockCount() == 0) {
							
				if (frame.getPage() == null) {
					if(freeFrame == null && frame != null){
						freeFrame = frame;
						break;
					}
				}				
				else {
					if (!frame.isDirty()) {
						if(notDirtyFrame == null){
							notDirtyFrame = frame;						
						}
					} 
					else {
						if(victmFrame == null){
							victmFrame = frame;
						}

					}
				}
			}
			
			i++;
		}
		

		frameofWork = freeFrame;
		
		if(freeFrame != null){
			frameofWork = freeFrame;
		}
		else{		
			
			if(notDirtyFrame != null){
				frameofWork= notDirtyFrame;
			}
			else{				
				frameofWork = victmFrame;
			}
		}
		
		
		if(frameofWork == null){/* Caso não tenha frames disponíveis */
			
			page.notifyThreads();
			pfEvent.notifyThreads();
			ThreadCB.dispatch();
			return GlobalVariables.NotEnoughMemory;			
		}
		
		else
		{
						
			frameofWork.setReserved(thread.getTask());
					
			page.setValidatingThread(thread);
			
			PageTableEntry paginaRetirada = frameofWork.getPage();
			
			if (paginaRetirada != null) {

				if (frameofWork.isDirty()) {
					
					/*swap in*/
					TaskCB task = thread.getTask();
					PageTableEntry pagina = frameofWork.getPage();
					task.getSwapFile().write(pagina.getID(), pagina, thread);
					
					
					if (thread.getStatus() == GlobalVariables.ThreadKill) {/*devido a demora no write*/						
						page.notifyThreads();
						page.setValidatingThread(null);
						pfEvent.notifyThreads();
						ThreadCB.dispatch();						
						return GlobalVariables.FAILURE; 
					}
					
					frameofWork.setDirty(false);
				}
				
				frameofWork.setReferenced(false);
				
				paginaRetirada.setValid(false);
				paginaRetirada.setFrame(null);
				
			//	if (paginaRetirada.getTask().getStatus() != TaskTerm) {
				//	frameofWork.setPage(null);
				//	}
				
			}

			page.setFrame(frameofWork);
			
			TaskCB task = thread.getTask();
			task.getSwapFile().read(page.getID(), page, thread);

			if (thread.getStatus() == GlobalVariables.ThreadKill) { /*devido a demora no read*/
				//if ((frameofWork.getPage() != null)	&& (frameofWork.getPage().getTask() == thread.getTask())) {
				frameofWork.setPage(null);
				//}
				page.notifyThreads();
				page.setValidatingThread(null);
				page.setFrame(null);
				pfEvent.notifyThreads();
				ThreadCB.dispatch();
				
				return FAILURE;
			}

			frameofWork.setPage(page);
			page.setValid(true);
			
			page.setValidatingThread(null);
			//		if (frameofWork.getReserved() == page.getTask()) {
			frameofWork.setUnreserved(page.getTask());
		//	}
			page.notifyThreads();
			pfEvent.notifyThreads();
			ThreadCB.dispatch();

			return SUCCESS;
		} 
    }

}

