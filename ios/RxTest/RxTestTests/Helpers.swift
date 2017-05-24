
import Foundation
import RxSwift
import RxBlocking
import XCTest

extension ObservableType where E : Comparable {
    func assertValues(values: E...) {
        let res = try! self.toBlocking().toArray()
        XCTAssert(res.elementsEqual(values))
    }
}

extension ObservableType {
    func doOnNext(_ code: @escaping (E) throws -> Void) -> Observable<Self.E> {
        return self.do(onNext: code)
    }
    
    func doOnEach(_ code: @escaping (RxSwift.Event<E>) -> Void) -> Observable<Self.E> {
        return self.do(onNext: { code(RxSwift.Event.next($0)) },
                onError: { code(RxSwift.Event.error($0)) },
                onCompleted: { code(RxSwift.Event.completed) },
                onSubscribe: nil, onSubscribed: nil, onDispose: nil)
    }
    
    func doOnComplete(_ code: @escaping () throws -> Void) -> Observable<Self.E> {
        return self.do(onCompleted: code)
    }
    
    func subscribeNext(_ code: @escaping (E) -> Void) -> Disposable {
        return self.subscribe(onNext: code)
    }
}

func pp(_ msg: String) {
    let formatter = DateFormatter()
    formatter.dateFormat = "mm:ss.SSS"
    let date = formatter.string(from: Date())
    let threadName = Thread.current.name
    print(" [\(date)] [\(String(describing: threadName))] \(msg)")
}

//
// https://github.com/zhuhaow/CountdownLatch/blob/master/CountdownLatch/CountdownLatch.swift
//
class CountdownLatch {
    var count: Int
    private let dispatch_queue = DispatchQueue(label: "CountdownQueue", attributes: [])
    let semaphore = DispatchSemaphore(value: 0)
    
    init?(count: Int) {
        guard count > 0 else {
            return nil
        }
        
        self.count = count
    }
    
    func countdown() {
        dispatch_queue.async() {
            self.count -= 1
            if self.count == 0 {
                self.semaphore.signal()
            }
        }
    }
    
    func await() {
        _ = semaphore.wait(timeout: DispatchTime.distantFuture)
    }
}

class Schedulers {
    static let serial = SerialDispatchQueueScheduler(qos: .default)
}
