import XCTest
import RxSwift
import RxBlocking
@testable import RxTest

class ObservableBasicTest: XCTestCase {
    
    override func setUp() {
        super.setUp()
    }
    
    override func tearDown() {
        super.tearDown()
    }
    
    func testDelay() {
        
        //  [54:21.045] [Optional("")] delay.before delay : next(10)
        //  [54:21.046] [Optional("")] delay.before delay : completed
        //  [54:22.046] [Optional("")] delay.after delay : next(10)
        //  [54:22.046] [Optional("")] delay.subscribe : 10
        //  [54:22.046] [Optional("")] delay.after delay : completed
        
        let latch = CountdownLatch(1)
        
        _ = Observable.just(10)
            .doOnEach({ pp("delay.before delay : \($0)") })
            .delay(1, scheduler: Schedulers.serial)
            .doOnEach({ pp("delay.after delay : \($0)") })
            .doOnComplete({ latch.countdown() })
            .subscribeNext { pp("delay.subscribe : \($0)") }
        
        latch.await()
    }
    
    func testTakeWhileTakeUntil() {
        
        //  [26:02.270] [Optional("")] 0
        //  [26:02.767] [Optional("")] 1
        //  [26:03.265] [Optional("")] 2
        //  [26:03.766] [Optional("")] 3
        //  [26:04.266] [Optional("")] 4
        
        Observable<Int>.interval(0.5, scheduler: Schedulers.serial)
            .takeWhile({ $0 < 5 })
//            .takeUntil({ $0 > 2 }) // RxSwift does not have takeUntil /w predicate.
            .blockingForEach({ pp("\($0)") })
    }
    
    func testRepeat() {
        
        //  [37:04.015] [Optional("")] 3
        //  [37:04.015] [Optional("")] 3
        //  [37:04.015] [Optional("")] 3
        //  [37:04.015] [Optional("")] 3
        //  [37:04.015] [Optional("")] 3
        //
        // RxSwift 에는 ObservableType.repeat 이 없다. 대신 Observable.repeatElement 가 있긴 하다.

        Observable.repeatElement(3)
            .take(5)
            .dump()
    }
    
    func testScan() {
        
        //  [39:31.667] [Optional("")] doOnEach : next(0)
        //  [39:31.668] [Optional("")] scan => 0 + 0
        //  [39:31.668] [Optional("")] 0
        //  [39:31.761] [Optional("")] doOnEach : next(1)
        //  [39:31.761] [Optional("")] scan => 0 + 1
        //  [39:31.761] [Optional("")] 1
        //  [39:31.861] [Optional("")] doOnEach : next(2)
        //  [39:31.861] [Optional("")] scan => 1 + 2
        //  [39:31.861] [Optional("")] 3
        //  [39:31.961] [Optional("")] doOnEach : next(3)
        //  [39:31.961] [Optional("")] scan => 3 + 3
        //  [39:31.961] [Optional("")] 6
        //  [39:32.061] [Optional("")] doOnEach : next(4)
        //  [39:32.061] [Optional("")] scan => 6 + 4
        //  [39:32.061] [Optional("")] 10
        //  [39:32.161] [Optional("")] doOnEach : next(5)
        //  [39:32.161] [Optional("")] scan => 10 + 5
        //  [39:32.162] [Optional("")] 15
        //  [39:32.261] [Optional("")] doOnEach : next(6)
        //  [39:32.262] [Optional("")] scan => 15 + 6
        //
        // RxSwift 의 scan 에는 seed value 가 무조건 있게 되어 있다. 사실상 RxJava2 에서의 reduce와 대응되는 것으로 보인다.
        
        Observable<Int>.interval(0.1, scheduler: Schedulers.serial)
            .doOnEach({ pp("doOnEach : \($0)") })
            .scan(0, accumulator: { acc, value in
                pp("scan => \(acc) + \(value)")
                return acc + value
            })
            .takeWhile({ $0 < 20 })
            .dump()
    }
    
    func testBasicMap() {
        generate("1,2,3", 100)
            .map { $0 + 10 }
            .doOnNext { pp("subscribe : \($0)") }
            .assertValues(values: 11, 12, 13)
    }
    
    func testWindow() {
        
        //  [47:55.472] [Optional("")] Created a new observable! 1
        //  [47:55.476] [Optional("")] Emitting value : 1
        //  [47:55.477] [Optional("")] [1] subscription : 1
        //  [47:55.977] [Optional("")] Emitting value : 2
        //  [47:55.977] [Optional("")] [1] subscription : 2
        //  [47:56.477] [Optional("")] Emitting value : 3
        //  [47:56.477] [Optional("")] [1] subscription : 3
        //  [47:56.977] [Optional("")] Emitting value : 4
        //  [47:56.977] [Optional("")] [1] subscription : 4
        //  [47:57.475] [Optional("")] [1] Completed.
        //  [47:57.475] [Optional("")] Created a new observable! 2
        //  [47:57.477] [Optional("")] Emitting value : 5
        //  [47:57.477] [Optional("")] [2] subscription : 5
        //  [47:57.977] [Optional("")] Emitting value : 6
        //  [47:57.978] [Optional("")] [2] subscription : 6
        //  [47:58.478] [Optional("")] Emitting value : 7
        //  [47:58.478] [Optional("")] [2] subscription : 7
        //  [47:58.478] [Optional("")] [2] Completed.
        //
        // RxSwift의 window는 count OR time-elapsed 이다.
        // 마지막 종료 observable 은 다 채우지 않아도 complete 된다.
        
        var counter = 1
        generate("1,2,3,4,5,6,7", 500)
            .doOnNext({ pp("Emitting value : \($0)")})
            .window(timeSpan: 2, count: 5, scheduler: Schedulers.serial)
            .blockingForEach({ source in
                let myCounter = counter
                counter = counter + 1
                pp("Created a new observable! \(myCounter)")
                _ = source
                    .doOnComplete { pp("[\(myCounter)] Completed.") }
                    .subscribeNext {
                        pp("[\(myCounter)] subscription : \($0)")
                }
            })
    }
    
}
