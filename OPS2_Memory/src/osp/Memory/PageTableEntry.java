package osp.Memory;

import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;

public class PageTableEntry extends IflPageTableEntry {

	public PageTableEntry(PageTable ownerPageTable, int pageNumber) {
		super(ownerPageTable,pageNumber);
	}

	public int do_lock(IORB iorb) {
		ThreadCB thread = iorb.getThread();
		
		if (isValid()) { // Se a página está na memória
			getFrame().incrementLockCount();
			return SUCCESS;
		}
		else { // Se a página não está na memória > PageFault
			if (getValidatingThread() == null) { // Se a página não estava em PageFault
				int i = PageFaultHandler.handlePageFault(thread, MemoryLock, this);
				if (i == FAILURE) {
					return FAILURE;
				}
				else {
					getFrame().incrementLockCount();
					return SUCCESS;
				}
			}
			else if (getValidatingThread() == thread) { // PageFault da mesma thread
				getFrame().incrementLockCount();
				return SUCCESS;
			}
			else { // PageFault de outra thread
				thread.suspend(this);
				return FAILURE;
			}
		}
	}

	public void do_unlock() {
		getFrame().decrementLockCount();
	}
}
