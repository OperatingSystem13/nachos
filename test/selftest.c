#include "syscall.h"

int
main()
{
    creat("2.txt");
    creat("3.txt");
    creat("4.txt");
    creat("5.txt");
    creat("6.txt");
    creat("7.txt");
    creat("8.txt");
    creat("9.txt");
    creat("10.txt");
    creat("11.txt");
    creat("12.txt");
    creat("13.txt");
    creat("14.txt");
    creat("15.txt");
    creat("16.txt");
    close(3);
    close(4);
    close(5);
    unlink("6.txt");
    unlink("7.txt");
    unlink("8.txt");
    char buffer[64];
    read(open("0input.txt"), buffer, 64);
    write(open("0output.txt"), buffer, 64);
    write(1, buffer, 64);
    read(0, buffer, 4);
    write(creat("0receiver.txt"), buffer, 4);
    creat("17.txt");
    creat("18.txt");
    creat("19.txt");
    creat("20.txt");
    
   /* unlink("2.txt");
    unlink("3.txt");
    unlink("4.txt");
    unlink("5.txt");
    unlink("9.txt");
    unlink("10.txt");
    unlink("11.txt");
    unlink("12.txt");
    unlink("13.txt");
    unlink("14.txt");
    unlink("15.txt");
    unlink("16.txt");
    unlink("17.txt");
    unlink("18.txt");
    unlink("19.txt");
    unlink("20.txt");*/
    halt();
    /* not reached */
}
