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

import osp.IFLModules.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**
* The timer interrupt handler.  This class is called upon to handle timer
* interrupts.
*/
public class TimerInterruptHandler extends IflTimerInterruptHandler {
	
	/**
	* This basically only needs to reset the times and dispatch another
	* process.
	*/
	public void do_handleInterrupt() {
		MyOut.print(this, " ====>>> timer");
		HTimer.set(10);
		ThreadCB.dispatch();
	}
	
	/*
	* Feel free to add methods/fields to improve the readability of your code
	*/
}

/*
* Feel free to add local classes to improve the readability of your code
*/
