package osp.Memory;

import osp.Tasks.*;
import osp.IFLModules.*;

public class PageTable extends IflPageTable {

	public PageTable(TaskCB ownerTask) {
		super(ownerTask);
		
		int maxPages = (int)Math.pow(2, MMU.getPageAddressBits()); // Máximo de páginas
		pages = new PageTableEntry[maxPages]; // Vetor de páginas
		for (int i = 0; i < pages.length; i++) {
			pages[i] = new PageTableEntry(this, i);
		}
	}

	public void do_deallocateMemory() {
		for (int i = 0; i < pages.length; i++) {
			if (pages[i].getFrame() != null) {
				pages[i].getFrame().setDirty(false);
				pages[i].getFrame().setReferenced(false);
				pages[i].getFrame().setPage(null);
			}
		}
	}
}
