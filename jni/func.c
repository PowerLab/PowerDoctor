#include "func.h"

void do_swapint(int *tmpa, int *tmpb)
{
	int swapint = 0;
	swapint = *tmpa;
	*tmpa =  *tmpb;
	*tmpb = swapint;
}

void do_swapptr(void **tmpa, void **tmpb)
{
	void *swapptr = (void *)0;

	swapptr = *tmpa;
	*tmpa = *tmpb;
	*tmpb = swapptr;

	return;
}
