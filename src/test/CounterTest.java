package test;

import java.util.concurrent.atomic.AtomicInteger;

public class CounterTest {
  AtomicInteger counter = new AtomicInteger(0);

  public void count() {
    int result;
    boolean flag;
    result = counter.get();
    do {
      
      // �ϵ�
      // ���߳���, compareAndSet������ԶΪtrue,
      // ���߳���, ����result����compareʱ, counter���ܱ������߳�set����ֵ, ��ʱ��Ҫ������ȡһ���ٱȽ�,
      // �������û���õ����µ�ֵ, ��һֱѭ����ȥ, ֱ���õ����µ��Ǹ�ֵ
      //flag = counter.compareAndSet(result, result + 1);
      result++;
      System.out.println(result);
      } while (true);
   //return 0;
 }

  public static void main(String[] args) {
    final CounterTest c = new CounterTest();
    new Thread() {
      @Override
      public void run() {
    	  c.count();
        //System.out.println(c.count());
      }
    }.start();

    new Thread() {
      @Override
      public void run() {            	  
        c.count();
      }
    }.start();
//
//    new Thread() {
//      @Override
//      public void run() {
//        c.count();
//      }
//    }.start();
  }
}