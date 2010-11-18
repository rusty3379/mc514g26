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

import java.util.*;
import osp.IFLModules.*;
import osp.Resources.RRB;
import osp.Resources.ResourceTable;
import osp.Threads.*;
import osp.Utilities.GlobalVariables;
import osp.Memory.*;

public class ResourceCB extends IflResourceCB {
	
	private static ArrayList<ThreadCB> ListaThreads;
	private static ArrayList<RRB> ListaRRB;
		
	public ResourceCB(int qty) {
		super(qty);
	}
	
	public static void init() {
		ResourceCB.ListaRRB = new ArrayList<RRB>();
		ResourceCB.ListaThreads = new ArrayList<ThreadCB>();
	}
	
	public RRB do_acquire(int quantity) {
		
		if (quantity > this.getAvailable() || quantity < 0)
			return null;
		
		ThreadCB thread = MMU.getPTBR().getTask().getCurrentThread();
		
		if (this.getAllocated(thread) + quantity > this.getMaxClaim(thread))
			return null;
		
		ResourceCB.ListaThreads.add(thread);
		
		RRB request = new RRB(thread, this, quantity);
		
		if (IflResourceCB.getDeadlockMethod() == GlobalVariables.Avoidance) {
			if (ResourceCB.banker()) {
				request.setStatus(GlobalVariables.Granted);
				request.grant();
			}
			else {
				request.setStatus(GlobalVariables.Suspended);
				ResourceCB.ListaRRB.add(request);
				thread.suspend(request);
			}
		}
		else if (IflResourceCB.getDeadlockMethod() == GlobalVariables.Detection) {
			if (this.getAvailable() >= quantity) {
				request.setStatus(GlobalVariables.Granted);
				request.grant();
			}
			else if (this.getTotal() < quantity) {
				return null;
			}
			else {
				request.setStatus(GlobalVariables.Suspended);
				ResourceCB.ListaRRB.add(request);
				thread.suspend(request);
			}
		}
		return request;
	}
	
	public static Vector<ThreadCB> do_deadlockDetection() {
		int num_RRBs = ResourceCB.ListaRRB.size();
		int num_threads = ResourceCB.ListaThreads.size();
		int[] Work = new int[ResourceTable.getSize()]; 
		boolean[] Finish = new boolean[num_threads]; 
		int[][] Request = new int[num_threads][ResourceTable.getSize()];
		
		for (int i = 0; i < ResourceCB.ListaRRB.size(); i++) {
			int type = -1;
			RRB request = ResourceCB.ListaRRB.get(i); 
			for (int j = 0; j < ResourceTable.getSize(); j++) { 
				if (ResourceCB.ListaRRB.get(i).getResource() == ResourceTable.getResourceCB(j))
					type = j;
			}
			int thread_int = -1;
			ThreadCB thread = request.getThread();
			for (int j = 0; j < ResourceCB.ListaThreads.size(); j++) {
				if (ResourceCB.ListaThreads.get(j) == thread)
					thread_int = j;
			}
			Request[thread_int][type] = request.getQuantity();
		}
		
		for (int i = 0; i < ResourceTable.getSize(); i++)
			Work[i] = ResourceTable.getResourceCB(i).getAvailable();
		
		for (int i = 0; i < num_threads; i++) {
			Finish[i] = true;
			for (int j = 0; j < ResourceTable.getSize(); j++)
				if (ResourceTable.getResourceCB(j).getAllocated(ResourceCB.ListaThreads.get(i)) > 0)
					Finish[i] = false;
		}
		
		boolean such_index = false;
		boolean first_run = true;
		while (first_run || such_index) {
			such_index = false;
			first_run = false;
			for (int i = 0; i < Finish.length; i++) {
				if ( Finish[i] == false ) {
					boolean request_i_less_work = true;
					for (int j = 0; j < num_RRBs; j++)
						if ( Request[i][j] > Work[j] )
							request_i_less_work = false;
					if ( request_i_less_work == true) {
						such_index = true;
						for (int k = 0; k < Work.length; k++) {
							Work[k] += ResourceTable.getResourceCB(k).getAllocated(ResourceCB.ListaThreads.get(i));
							Finish[i] = true;
						}
						break;
					}
				}
			}
		}
		
		Vector<ThreadCB> vec = new Vector<ThreadCB>();
		
		if (such_index == false)
			for (int i = 0; i < Finish.length; i++)
				if (Finish[i] == false)
					vec.add(ResourceCB.ListaThreads.get(i));
		
		if (vec.size() == 0)
			return null;
		
		Vector<ThreadCB> newvec = vec;
		
		while (newvec != null) {
			ThreadCB thread = newvec.get(0);
			thread.kill();
			newvec = ResourceCB.do_deadlockDetection();
		}
		
		ResourceCB.grant_RRBs();
		
		return vec;
	}
	
	public static void do_giveupResources(ThreadCB thread) {
		
		for (int i = 0; i < ResourceTable.getSize(); i++) {
			ResourceCB res = ResourceTable.getResourceCB(i);
			res.setAvailable(res.getAvailable() + res.getAllocated(thread));
			res.setAllocated(thread, 0);
		}
		
		for (int i = 0; i < ResourceCB.ListaRRB.size(); i++)
			if (ResourceCB.ListaRRB.get(i).getThread() == thread)
				ResourceCB.ListaRRB.remove(i);
		
		ResourceCB.grant_RRBs();
	}
	
	public void do_release(int quantity) {
		
		if (quantity >= 0) {
			ThreadCB thread = MMU.getPTBR().getTask().getCurrentThread();
			this.setAvailable(this.getAvailable() + quantity);
			this.setAllocated(thread, this.getAllocated(thread) - quantity);
			ResourceCB.grant_RRBs();
		}
	}
	
	public static void atError() {
		// No code
	}
	
	public static void atWarning() {
		// No code
	}
	
	private static void grant_RRBs() {
		
		int num_RRBs = ResourceCB.ListaRRB.size(); 
			
		for (int i = 0; i < num_RRBs; i++) {
			int type = -1;
			RRB request = ResourceCB.ListaRRB.get(i);
			for (int j = 0; j < ResourceTable.getSize(); j++)
				if (ResourceCB.ListaRRB.get(i).getResource() == ResourceTable.getResourceCB(j))
					type = j;
			
			ResourceCB res = ResourceTable.getResourceCB(type);
			
			if (request.getQuantity() <= res.getAvailable() && request.getQuantity() >= 0) {
				res.setAvailable(res.getAvailable() - request.getQuantity());
				res.setAllocated(request.getThread(), res.getAllocated(request.getThread()) + request.getQuantity());
				request.setStatus(GlobalVariables.Granted);
				request.grant();
				ResourceCB.ListaRRB.remove(i);
			}
		}
	}
	
	public static boolean banker() {
		int num_threads = ResourceCB.ListaThreads.size();
		int[] Work = new int[ResourceTable.getSize()]; 
		boolean[] Finish = new boolean[num_threads]; 
		int[][] Max = new int[num_threads][ResourceTable.getSize()];
		int[][] Request = new int[num_threads][ResourceTable.getSize()];
		
		for (int i = 0; i < num_threads; i++)
			for (int j = 0; j < ResourceTable.getSize(); j++)
				Max[i][j] = ResourceTable.getResourceCB(j).getMaxClaim(ResourceCB.ListaThreads.get(i));
		
		for (int i = 0; i < ResourceCB.ListaRRB.size(); i++) {
			int type = -1;
			RRB request = ResourceCB.ListaRRB.get(i);
			
			for (int j = 0; j < ResourceTable.getSize(); j++)
				if (ResourceCB.ListaRRB.get(i).getResource() == ResourceTable.getResourceCB(j))
					type = j;
			
			int thread_int = -1;
			ThreadCB thread = request.getThread();
			
			for (int j = 0; j < ResourceCB.ListaThreads.size(); j++)
				if (ResourceCB.ListaThreads.get(j) == thread)
					thread_int = j;
			
			Request[thread_int][type] = request.getQuantity();
		}
		
		for (int i = 0; i < ResourceTable.getSize(); i++)
			Work[i] = ResourceTable.getResourceCB(i).getAvailable();
		
		for (int i = 0; i < num_threads; i++)
			Finish[i] = false;
		
		boolean such_i = true;
		while (such_i) {
			such_i = false;
			for (int i = 0; i < num_threads; i++) {
				if (Finish[i] == false) {
					boolean need_i_less_work = true;
					for (int j = 0; j < ResourceTable.getSize(); j++) {
						int need_i_j = Max[i][j] - ResourceTable.getResourceCB(j).getAllocated(ResourceCB.ListaThreads.get(i));
						if ( need_i_j > Work[j] )
							need_i_less_work = false;
					}
					if (need_i_less_work) {
						such_i = true;
						for (int j = 0; j < ResourceTable.getSize(); j++)
							Work[j] += ResourceTable.getResourceCB(j).getAllocated(ResourceCB.ListaThreads.get(i));
						Finish[i] = true;
						break;
					}
				}
			}
		}
		
		for (int i = 0; i < num_threads; i++)
			if (Finish[i] == false)
				return false;
		
    	return true;
    }
}
