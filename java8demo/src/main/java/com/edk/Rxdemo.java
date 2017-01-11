package com.edk;
/**
 * Created by Edward on 2016/11/25 14:45.
 * 自动生成的模版
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static java.lang.Thread.sleep;
import static rx.Observable.just;

/**
 * @author Edward
 * @revised by
 * Ddddddddd
 * <p>
 * Ower PDD
 */
public class Rxdemo {


    /**
     * <p>{@link Observable#from(Object[])} -----------</p>
     * <p>{@link Observable#defer(Func0)} ------------ 把一般对象 封装成可以 observables</p>
     * <p>{@link Observable#flatMap(Func1)} ------------ 对原observable 使用函数变换 封装成新的 observable 再发射</p>
     *
     * @param words
     */
    public void sayHello(String... words) {

        ArrayList<String[]> list = new ArrayList<>();
        list.add(values);
        list.add(names);
        //在没有使用 flagMap之前 直接发射是 string[]  使用后，把 string[]在拆分成单个 string emit
        Observable.from(list).flatMap(Observable::from).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                System.out.println("========" + s);
            }
        });

        Observable.from(values).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                System.out.println(s + ",");
            }
        });


        //必须有 一个 订阅者(subscribe something) 才会触发 被观察对象 发射
        Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                //默认同一线程，需要异步操作 需要 设置观察 模式为异步 observeOn(Schedulers.newThread())
                int i = 0;
                while (i < 10) {
                    try {
                        i++;
                        Thread:
                        sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return just("count " + i);

            }
        }).observeOn(Schedulers.newThread()).subscribe(new Action1<String>() {//new observable(onError onCompleted onNext)
            @Override
            public void call(String s) {
                System.out.print("defer->onNexts:" + s + "\n");
            }
        });


        // s指定是 被观察的 values数组
        Subscription mSub = just(values).subscribe(s -> {
            System.out.println("onNext:" + s.length);
        }, e -> {
            System.err.print("error!");
        }, new Action0() {
            @Override
            public void call() {
                System.out.println("\n onCompleted");
            }
        });
        //对已经订阅 可观察对象的 观察者进行统一管理
        cs.add(mSub);


        //        Observable.from(names);
        //        Observable.just(names);


        //???什么效果
        //        Observable.timer(3000, TimeUnit.MILLISECONDS).subscribe(new Observer<Long>() {
        //          String msg="";
        //            @Override
        //            public void onCompleted() {
        //                msg +="onCompleted,";
        //                System.out.print(msg);
        //            }
        //
        //            @Override
        //            public void onError(Throwable e) {
        //                msg +="onError,";
        //            }
        //
        //            @Override
        //            public void onNext(Long aLong) {
        //                msg +="onNext,";
        //                System.out.print(msg);
        //            }
        //        });


    }


    CompositeSubscription cs = new CompositeSubscription();

    /**
     * <p>{@link Observable#error(Throwable)} ------------- 构造一个error 的observable</p>
     * <p>{@link Observable#distinct(Func1)} ------对每个item 项进行特定函数约束判断 </p>
     * <p>{@link Observable#distinctUntilChanged()} -------------- 对前后相同的 item项去重，如果相同item但存在间隔 无法剔除</p>
     * <p>{@link Observable#skip(int)} --------------跳过前x个 item</p>
     * <p> {@link Observable#elementAt(int)} ------------发射第 x个 item</p>
     * <p>{@link Observable#take(long, TimeUnit)} -------------------在等待固定延时之后 发射数据</p>
     */
    public void sayHelloAsync() {

        //去重并跳转前2个observable emit余下的observable
        just("1", "1", "2", "11", "22", "444", "22", "55555", "666666", "7777777")
                //.distinctUntilChanged()
                .distinct(new Func1<String, Boolean>() {

                    @Override
                    public Boolean call(String s) {
                        //通过判断 一组observable的 string.length 逻辑 进行"去重"操作
                        boolean b;
                        try {
                            //去重条件：奇数/偶数
                            int val = Integer.parseInt(s);
                            b = val % 2 == 0;
                        } catch (NumberFormatException e) {
                            b = false;
                        }
                        System.out.println("distinct result:" + b);
                        return b;


                        //return s.length();
                    }//跳转去重后发送的 一组observable 的第一个 并只发送index=1的observable,skip() elementAt()调用顺序还有关系?
                })
                .subscribe(new Action1<String>() {//.skip(1).elementAt(1)
                    @Override
                    public void call(String string) {// 11 333
                        System.out.println("onNext->arrays:" + string);
                    }
                });


        // hos to use skip/take: just() 操作符传入 一组 observable
        just(nums, nums).map(new Func1<ArrayList, String>() {
            @Override
            public String call(ArrayList arrayList) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "result first num:" + arrayList.get(0) + "-" + arrayList.size();
            }//返回 nums
        }).asObservable().take(2, TimeUnit.SECONDS).subscribe(new Observer<String>() {

            @Override
            public void onCompleted() {
                System.out.println("completed111");//be invoke??
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("onError111:" + e);
            }

            @Override
            public void onNext(String s) {
                System.out.println("onNext111:" + s);
            }

        });


        //自定义 订阅者 emit 发射机制 但是必须遵循rx 的调用原则
        //        Observable.create(new Observable.OnSubscribe<String>() {
        //            @Override
        //            public void call(Subscriber<? super String> subscriber) {
        //                //
        //
        //                try {
        //                    int i = Integer.parseInt("10s");
        //                    sleep(2000);
        //                    if (!subscriber.isUnsubscribed()){
        //                        subscriber.onNext(i + "");
        //                        subscriber.onCompleted();
        //                    }
        //                } catch (InterruptedException e) {
        //                        e.printStackTrace();
        //                }catch (NumberFormatException e){
        //                   // e.printStackTrace();
        //                    subscriber.onError(e);
        //                    // 在特定的调度器上，   .observeOn(Schedulers.newThread())
        //                    // 该异常 为什么不被触发?
        //                }
        //
        //            }
        //        }).subscribe(new Subscriber<String>() {
        //            @Override
        //            public void onCompleted() {
        //                System.out.println("onCompleted()11");
        //            }
        //
        //            @Override
        //            public void onError(Throwable e) {
        //                System.out.println("onError()11");
        //            }
        //
        //            @Override
        //            public void onNext(String s) {
        //                System.out.println("onNext()11");
        //            }
        //        });

        //只发射 onError 的observable
        Observable.error(new NullPointerException("custom npe")).subscribe(new Action1<Object>() {
            @Override
            public void call(Object o) {
                System.out.println("HA:" + o);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable s) {
                System.err.println("HA:" + s.getMessage());
            }
        });


    }


    /**
     * <p>{@link Observable#startWith(Iterable)} ------ 在emit 源observable之前 </p>
     * <p>{@link Observable#concatWith(Observable)} ---- 在 源observable 队列中添加 新的同类型的observable 再一起发送</p>
     * <p>{@link Observable#map(Func1)} -----------对 源observable 对每一个 item 进行函数变换后 再发射</p>
     * <p>{@link Observable#publish(Func1)}??? -------- 该 函数变换中 转换为 "热" observable 无须 connect 做为触发媒介</p>
     * <p>{@link Observable#publish()} ------------- 类似于 from ，just等，但只是创建一个 "冷" observable 需要使用 connect() 激活发射</p>
     */
    public void sayHelloLast() {
        //startWith concatWith 在发射的特定的序列（just(xxxx)）之前 和之后 都加入 其他组的observable
        Observable concat = Observable.from(new String[][]{{"acFun", "bilibili"}, {"Pixvix111"}});
        //注意 concatMap 传递的是 T[] , 之前just(T) 和 startWith(T) 使用的就是 string[] 所以 concatMap需要使用 string[][]
        just(values).startWith(new String[]{"bosher", "cliton"}).concatWith(concat).map(new Func1<String[], ArrayList>() {
            @Override
            public ArrayList call(String[] strings) {
                ArrayList list = new ArrayList();
                for (String s : strings) {
                    try {
                        sleep(10);
                        if (s.length() > 4)
                            list.add(s);
                    } catch (InterruptedException e) {

                    }
                }
                return list;
            }//.subscribeOn(xxx)添加 就无法运行
        }).observeOn(Schedulers.io()).subscribe(new Observer<ArrayList>() {
            @Override
            public void onCompleted() {
                System.out.println("onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("onError:" + e.toString());
            }

            @Override
            public void onNext(ArrayList arrayList) {
                System.out.println("onNext:" + arrayList);
            }
        });

        System.out.println("method sayHelloLast over!");

        ConnectableObservable<String> connObser = Observable.just("monday",
                "tuesday",
                "wednesday", "thursday", "friday", "saturday", "sunday")
                .publish();

        Subscription subcription = connObser.subscribe(s -> {
            //lamda expression
            System.out.print(" => " + s);
        });

        //trigger observable to emit data
        //connObser.connect();

        Observable.just(names, values).publish(new Func1<Observable<String[]>, Observable<String>>() {
            @Override
            public Observable<String> call(Observable<String[]> observable) {
                //把原先发射连个 string array 转成 发射单一的 string item
                return observable.flatMap(Observable::from);
            }
        }).subscribe(o -> System.out.println("o:" + o));


        //                .subscribe(new Action1<String>() {
        //            @Override
        //            public void call(String s) {
        //                System.out.print("=>" + s);
        //            }
        //        });
    }


    /**
     * <p>{@link Observable#groupBy(Func1)} ---------------</p>
     */
    public void sayHelloAgain() {
        //,Schedulers.newThread()
        //Observable.interval(1000, 2000, TimeUnit.MILLISECONDS)
        //map(this::combine) equal to this

        just(values).map(new Func1<String[], ArrayList<String>>() {

            @Override
            public ArrayList<String> call(String[] strings) {
                //string[] -> 使用函数变换 arraylist structure 默认不再任何调度器上执行函数变换
                return combine(strings);
            }//.skip(1).take(2) 该操作符 怎么使用?
        }).observeOn(Schedulers.io()).subscribe(new Observer<ArrayList<String>>() {
            @Override
            public void onCompleted() {
                System.out.println("onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                System.err.println("e:" + e.toString());
            }

            @Override
            public void onNext(ArrayList<String> strings) {
                System.out.println("list:" + strings);
            }
        });

        //System.out.println("do next job");


        just(values).groupBy(new Func1<String[], Observable<String>>() {


            @Override
            public Observable<String> call(String[] strings) {
                //抽取 奇数索引 拼接成字符串 进行分组 发送
                String s = "";
                String s1 = "";
                for (int i = 0; i < strings.length; i++) {
                    String x = strings[i];
                    if (i % 2 == 0) {
                        s += x;
                    } else {
                        s1 += x;
                    }
                }
                //  System.out.println("call:"+strings[3]+","+s);
                return Observable.just(s, s1);
            }
        }).subscribe(new Action1<GroupedObservable<Observable<String>, String[]>>() {
            @Override
            public void call(GroupedObservable<Observable<String>, String[]> observableGroupedObservable) {
                GrouopByEmit(observableGroupedObservable.count());
                System.out.println("GroupedObservable->onNext:" + observableGroupedObservable.count() + "," + observableGroupedObservable.getKey());
            }
        });


    }


    private void GrouopByEmit(Observable<Integer> count) {
        count.subscribe(arr ->
                System.out.print(arr + " # ")
        );


        List<String> items = new ArrayList<>();
        items.add("A");
        items.add("B");
        items.add("C");
        items.add("D");
        items.add("E");

        //only api24 that can support almost all java8 features
        //see
        //Output : A,B,C,D,E
        //items.forEach(item->System.out.println(item));

        //android 项目无法支持 全部特性
       // Arrays.asList( "a", "b", "d" ).forEach(e -> System.out.println(e) );
    }


    /**
     * @param who some to combine
     * @return
     */
    private ArrayList<String> combine(String[] who) {
        System.out.println("===" + who[0]);
        ArrayList list = new ArrayList();
        for (String s :
                who) {
            try {
                list.add("old:" + s);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }

        return list;
    }


    /**
     * <p>{@link Observable#takeUntil(Func1)} ---------------  当满足Func1所变换的条件，源observables 就不再发射 剩余的 item项</p>
     * <p>{@link Observable#takeWhile(Func1)} --------------- 对 源observable的只要 顺序的 每个 item 均满足某个 Fun1 条件就发射，当有一个item 不符合条件条件 将 结束，余下的item 不再发射</p>
     * <p>{@link Observable#timeout(long, TimeUnit)} ------- 统计每次发射前后2个 item间隔 如果超过指定 x(ms)还无法发射，就结束本次订阅，并触发 onError->TimeoutException</p>
     * <p>{@link Observable#skip(int)} ------------</p>
     * <p>{@link Observable#take(int)} --------只发送 前 x个 item</p>
     */
    private void sayHelloFinally() {
        //  System.out.println("sayHelloFinally");

        //name[] -> 600ms values[] -> 900ms
        Observable<String[]> o = just(names, names, values, names, values);
        boolean b = true;
        ConnectableObservable<String[]> connectableObservable;
        if (true) {
            connectableObservable = o.publish();
            //订阅2组observable name，values, xxx.skip(1).take(1).xx表示：跳过第一组name，再取第二组，第三组observable 进行发送
            final Subscription ss = connectableObservable.skip(1).take(3)
                    .map(arr -> {
                        try {
                            Thread:
                            sleep(100 * arr.length);
                        } catch (InterruptedException e) {
                            System.err.println("map() sleep:" + e.toString());
                        } finally {
                            System.out.println("map() finally:" + arr[0] + "," + arr.length);
                        }
                        return arr;
                    }).timeout(700, TimeUnit.MILLISECONDS)//控制每个item 执行操作到发射 耗时≤700ms
                    .doOnSubscribe(() -> {
                        System.out.println("doOnSubscribe");
                    })
                    .subscribe(new Subscriber<String[]>() {
                        @Override
                        public void onCompleted() {
                            System.out.println("onCompleted");
                        }

                        @Override
                        public void onError(Throwable e) {
                            System.err.println("onError() " + e.toString());
                        }

                        @Override
                        public void onNext(String[] strings) {
                            System.out.println(strings[0] + ",,,," + strings[1]);
                        }

                        @Override
                        public void onStart() {
                            super.onStart();
                            System.out.println("onStart");
                        }
                    });
            //触发 observable 开关
            connectableObservable.connect();

            //            connectableObservable.connect(new Action1<Subscription>() {
            //                @Override
            //                public void call(Subscription subscription) {
            //                    try {//在发射所有observable前，最后可以执行操作的地方
            //                        sleep(Math.random() > 0 ? 4000 : 2000);//反射执行操作 >timeout(3000) 将调用 观察者(即订阅者) onError()回调抛出异常
            //                        //subscription.unsubscribe();
            //                        //TODO 该方法什么作用
            //                        System.out.println("equal?" + ss + "," + subscription);
            //                    } catch (InterruptedException e) {
            //                        e.printStackTrace();
            //                    }
            //                }
            //            });
        } else {

            // 不需要使用 ConnectableObservable.Connect() 触发订阅时间
            //            o.publish(new Func1<Observable<String[]>, Observable<String[]>>() {
            //
            //                Observable<String[]> newOne;
            //
            //                @Override
            //                public Observable<String[]> call(Observable<String[]> original) {
            //                    //pattern1
            //                   newOne =Observable.concatEager(original,Observable.just(dummy));
            //                    //pattern2
            //                  // newOne =original.concatWith(Observable.just(dummy));
            //
            //                    //实现把 冷 -> 热 的转变
            //                    return newOne;
            //                }
            //            })


            o.publish(original -> original.concatWith(just(dummy))).timeout(1000, TimeUnit.MILLISECONDS)
                    //要手动 强转 所观察者订阅的类型 （如果 observable<T> 先前已经指定好 观察类型 ，代码模版会自动完成转换）
                    //                    .ofType(String[].class)
                    //                    .subscribe(new Action1<String[]>() {
                    //                        @Override
                    //                        public void call(String[] o) {
                    //
                    //                        }
                    //                    })

                    //代码模版有时候 无法自动强转为 所对应的订阅 observable 类型，请手动添加
                    .subscribe(new Subscriber<String[]>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            System.err.println(e.toString());
                        }

                        @Override
                        public void onNext(String[] arr) {
                            System.out.println(arr[0] + ",,,," + arr[1]);
                        }
                    });
        }

        System.out.println("--------------------------------");

        //延时 创建需要发送的observable
        Observable.defer(new Func0<Observable<List<Integer>>>() {
            @Override
            public Observable<List<Integer>> call() {
                return just(nums.subList(0, 3), nums.subList(0, 3), nums.subList(0, 3), nums.subList(0, 1));
            }
        }).takeWhile(integers -> {
            //takeWhile 在满足 特定情况下才会发射
            System.out.println("takeWhile:" + integers.size());
            return integers.size() >= 3;
        }).subscribe(intList -> {
                    System.out.println("interges:" + intList.toString());
                }, throwable ->
                        System.err.println(throwable.toString())
                , () -> System.out.println("onCompleted"));
    }


    public int mapApi(String[] str) {
        return Integer.parseInt(str[1]);
    }

    /**
     * @param str
     * @return
     */
    public Observable<Integer[]> flatMap(String[] str) {
        Integer[] array = new Integer[str.length];
        array[0] = Integer.parseInt(str[0]) + 1;
        array[1] = Integer.parseInt(str[1]) + 1;
        return just(array);
    }


    /**
     * <p>{@link Subscriber#onStart()} ---------- Observer 的子类，onStart 在 observable 被订阅 doOnSubscribe() 之后,立即回调</p>
     * <p>{@link Observable#doOnSubscribe(Action0)} ----- 该observable被 订阅后 最先发生</p>
     * <p>{@link Observable#doOnNext(Action1)} --------  优先于 onNext() 被调用</p>
     * <p>{@link Observable#doOnUnsubscribe(Action0)} -------- 手动调用 unSubscribe() 或者 onCompleted/onError 被回调/触发 之后（被调用）</p>
     * <p>{@link Observable#doOnError(Action1)} -------- 优先于 onError() 被调用</p>
     * <p>{@link Observable#from(Object[])} -------------- </p>
     * <p>{@link Observable#subscribe(Observer)} ------ 存在 onError onCompleted onNext 三个回调</p>
     *
     *
     */
    private void sayHelloTruly() {
        //map(this::mapApi)
        //        Observable.just(values, values, values).map(new Func1<String[], Integer>() {
        //            @Override
        //            public Integer call(String[] strings) {
        //                return Integer.parseInt(strings[1]);
        //            }
        //        }).take(5).subscribe(new Action1<Integer>() {
        //            @Override
        //            public void call(Integer r) {
        //                System.out.println("call:" + r);
        //            }
        //        });


        //method1:flatMap(this::flatMap)
        //method2:
        /**
         * .flatMap(new Func1<String[], Observable<Integer[]>>() {
        @Override public Observable<Integer[]> call(String[] strings) {
        return flatMap(strings);
        }
        })
         *
         * */
        //        Observable.just(values, values).flatMap(strings -> {
        //            //lamda 使用技巧
        //            //把 strings 做变换为 int[] 然后在包装成 observable 再次发送
        //            return Observable.just(numArr);
        //        }).subscribe(new Action1<int[]>() {
        //            @Override
        //            public void call(int[] integers) {
        //                System.out.println("call:" + integers[0] + "," + integers[1]);
        //            }
        //        });


        Observable.from(names).map(s -> {
            try {
                //4,4,4,5,6,5
                //if (s.length() % 2 == 1) "123".substring(10);
                Thread:sleep(1000);
            }catch (InterruptedException e){
                System.err.println(e.toString());
            }
            return s + "10086";
        }).timeout(3000,TimeUnit.MILLISECONDS).doOnNext(s -> {
            System.out.println("doOnNext():" + s);
        }).doOnError(t -> {
            System.err.println("doOnError():" + t.toString());
        }).doOnSubscribe(() -> {
            System.out.println("doOnSubscribe");
        }).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                System.out.println("doOnUnsubscribe");
            }
        }).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                //正常发射完毕 在回调该方法之后 就会触发 unsubscribe
                System.out.println("onCompleted");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("onError");
                //发射数据中途异常 在回调该方法之后 也将触发unsubscribe
            }

            @Override
            public void onNext(String s) {
                System.out.println("onNext:"+s);
            }

            @Override
            public void onStart() {
                System.out.println("onStart");
            }
        });


     new Thread(new Runnable() {
         @Override
         public void run() {

         }
     }).start();

        new Thread(()->System.out.println("great"));


    }


    public void backpressureStratgy(){

        //对不支持backpressure的操作符，通过onBackpressureDrop/onBackpressureBuffer
        Observable.range (1,10000).onBackpressureDrop ().observeOn (Schedulers.newThread ()).subscribe (new Subscriber<Integer> () {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {

                try {
                    sleep (500);
                    //request (integer.intValue ()>=60? Long.MAX_VALUE:3)

                    System.out.println("onBackpressureDrop:"+integer.intValue ());
                } catch (InterruptedException e) {
                    e.printStackTrace ();
                }finally {
                    request (1);
                }

            }

            @Override
            public void onStart() {
                request (1);
            }
        });
    }







    public final static String[] values = {"11", "22", "33", "44", "55", "66", "77", "22", "33"};
    public final static String[] names = {"jake", "mark", "lily", "Peter", "warton", "harry"};
    public final static String[] dummy = {"annoymous", "inner", "class"};
    public final static int[] numArr = {1, 2, 3, 4, 5, 6, 7, 8};

    ArrayList nums = new ArrayList<Integer>();


    public Rxdemo() {
        int i = 0;
        while (i < 50) {
            nums.add((i++));
        }
    }


    public static void main(String[] args) {
        Rxdemo rx = new Rxdemo();
        //rx.sayHello(names);
        //rx.sayHelloAsync();
        // rx.sayHelloLast();
        //rx.sayHelloAgain();
        //rx.sayHelloFinally();
       // rx.sayHelloTruly();
        rx.backpressureStratgy ();

    }





}
