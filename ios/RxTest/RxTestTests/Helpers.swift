
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
    
    func blockingForEach(_ code: @escaping (E) -> Void) {
        let latch = CountdownLatch(1)
        _ = self.doOnComplete { latch.countdown() }.subscribeNext(code)
        latch.await()
    }

    // 아래의 구현체는 실제 RxJava2 의 takeUntil 동작과 다르다. boundary condition 에 대해서 다르게 동작한다.
//    public func takeUntil(_ predicate: @escaping (Self.E) throws -> Bool) -> RxSwift.Observable<Self.E> {
//        return self.takeWhile({ try !predicate($0) })
//    }
    
    public func dump() {
        self.blockingForEach({ pp("\($0)") })
    }
}

func generateObservable<T>(list: [T?], delayInMS: Int = 1000) -> Observable<T> {
    return Observable.deferred {
        var observables = [Observable<T>]()
        for (index, value) in list.enumerated() {
            if let value = value {
                observables.append(Observable.just(value).delay(Double(index * delayInMS) / 1000.0, scheduler: Schedulers.serial))
            }
        }
        return Observable.merge(observables)
    }
}

func generate(_ streamString: String, _ delayInMs: Int = 1000) -> Observable<Int> {
    return generateObservable(list: streamString.components(separatedBy: ",").map { Int($0) }, delayInMS: delayInMs)
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
    
    init(_ count: Int) {
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
