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
        
        let latch = CountdownLatch(count: 1)
        
        _ = Observable.just(10)
            .doOnEach({ pp("delay.before delay : \($0)") })
            .delay(1, scheduler: Schedulers.serial)
            .doOnEach({ pp("delay.after delay : \($0)") })
            .doOnComplete({ latch?.countdown() })
            .subscribeNext { pp("delay.subscribe : \($0)") }
        
        latch?.await()
    }
}
