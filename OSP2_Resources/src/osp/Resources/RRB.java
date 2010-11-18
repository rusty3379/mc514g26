/*
* Grupo 26
* RA: 092549 - Paolo Barreto Nunes da Silva 
* RA: 097107 - Wellington da Silva Gomes
*
* Status: Final (sem comentarios)
*
* 25/10/2010
*/

package osp.Resources;

import osp.IFLModules.*;
import osp.Resources.ResourceCB;
import osp.Threads.*;

public class RRB extends IflRRB {
	
	public RRB(ThreadCB thread, ResourceCB resource,int quantity) {
		super(thread, resource, quantity);	
	}
	
	public void do_grant() {
		ResourceCB res = this.getResource();
		res.setAvailable(res.getAvailable() - this.getQuantity());
		res.setAllocated(this.getThread(),
				res.getAllocated(this.getThread()) + this.getQuantity());
		this.setStatus(Granted);
		this.notifyThreads();
	}
}
