import XCTest
import RxSwift
import RxBlocking
@testable import RxTest

class RxTestTests: XCTestCase {
    
    override func setUp() {
        super.setUp()
    }
    
    override func tearDown() {
        super.tearDown()
    }
    
    func testHowToTestObservable() {
        let res = try! Observable.just(10)
            .debug(" Emitting >>")
            .toBlocking()
            .toArray()
        
        XCTAssert(res.elementsEqual([10]))
    }
}
