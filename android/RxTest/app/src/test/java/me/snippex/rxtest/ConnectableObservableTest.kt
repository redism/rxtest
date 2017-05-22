package me.snippex.rxtest

import io.reactivex.disposables.Disposable
import org.junit.Test

class ConnectableObservableTest {

    @Test
    fun connectableBasic() {
        //  [48:05.882] [main] calling publish()
        //  [48:05.891] [main] calling first subscribe()
        //  [48:06.894] [main] calling second subscribe() after 1s
        //  [48:06.894] [main] calling connectableStream.connect()
        //  [48:06.940] [main] Subscribing to the stream.
        //  [48:06.943] [RxComputationThreadPool-1] Emitting 1
        //  [48:06.944] [RxComputationThreadPool-1] subscribe 1 : 1
        //  [48:06.944] [RxComputationThreadPool-1] subscribe 2 : 1
        //  [48:07.946] [RxComputationThreadPool-2] Emitting 2
        //  [48:07.947] [RxComputationThreadPool-2] subscribe 1 : 2
        //  [48:07.947] [RxComputationThreadPool-2] subscribe 2 : 2
        //  [48:08.447] [main] blocking subscription after 1.5s
        //  [48:08.947] [RxComputationThreadPool-3] Emitting 3
        //  [48:08.947] [RxComputationThreadPool-3] subscribe 1 : 3
        //  [48:08.947] [RxComputationThreadPool-3] subscribe 2 : 3
        //  [48:08.948] [main] 3
        //  [48:09.947] [RxComputationThreadPool-4] Emitting 4
        //  [48:09.947] [RxComputationThreadPool-4] subscribe 1 : 4
        //  [48:09.947] [RxComputationThreadPool-4] subscribe 2 : 4
        //  [48:09.947] [main] 4

        val stream = generate("1,2,3,4", 1000)
                .doOnSubscribe { pp("Subscribing to the stream.") }
                .printNext("Emitting ")

        pp("calling publish()")
        val connectableStream = stream.publish()

        pp("calling first subscribe()")
        connectableStream.subscribe { pp("subscribe 1 : $it") }

        Thread.sleep(1000)

        pp("calling second subscribe() after 1s")
        connectableStream.subscribe { pp("subscribe 2 : $it") }

        pp("calling connectableStream.connect()")
        connectableStream.connect()

        Thread.sleep(1500)
        pp("blocking subscription after 1.5s")
        connectableStream.print()
    }

    @Test
    fun connectableAutoConnect() {

        //  [51:51.884] [main] waiting 1s
        //  [51:52.894] [main] subscribing 1
        //  [51:52.897] [main] Waiting 1s
        //  [51:53.901] [main] subscribing 2
        //  [51:53.944] [main] subscribing..
        //  [51:53.947] [RxComputationThreadPool-1] Emitting 1
        //  [51:53.947] [RxComputationThreadPool-1] subscribe 1 : 1
        //  [51:53.947] [RxComputationThreadPool-1] subscribe 1 : 1
        //  [51:53.948] [main] Waiting 1s
        //  [51:54.952] [main] subscribing 3
        //  [51:54.952] [RxComputationThreadPool-2] Emitting 2
        //  [51:54.952] [RxComputationThreadPool-2] subscribe 1 : 2
        //  [51:54.952] [RxComputationThreadPool-2] subscribe 1 : 2
        //  [51:55.950] [RxComputationThreadPool-3] Emitting 3
        //  [51:55.951] [RxComputationThreadPool-3] subscribe 1 : 3
        //  [51:55.951] [RxComputationThreadPool-3] subscribe 1 : 3
        //  [51:55.951] [RxComputationThreadPool-3] subscribe 3 : 3
        //  [51:56.952] [RxComputationThreadPool-4] Emitting 4
        //  [51:56.952] [RxComputationThreadPool-4] subscribe 1 : 4
        //  [51:56.952] [RxComputationThreadPool-4] subscribe 1 : 4
        //  [51:56.952] [RxComputationThreadPool-4] subscribe 3 : 4

        val stream = generate("1,2,3,4", 1000)
                .doOnSubscribe { pp("subscribing..") }
                .printNext("Emitting ")
                .publish()
                .autoConnect(2)

        pp("waiting 1s")
        Thread.sleep(1000)
        pp("subscribing 1")
        stream.subscribe { pp("subscribe 1 : $it") }

        pp("Waiting 1s")
        Thread.sleep(1000)
        pp("subscribing 2")
        stream.subscribe { pp("subscribe 1 : $it") }

        pp("Waiting 1s")
        Thread.sleep(1000)
        pp("subscribing 3")
        stream.printNext("subscribe 3 : ").blockingSubscribe()
    }

    @Test
    fun autoConnectDisposableRefCount() {

        //  [59:20.769] [main] subscribing..
        //  [59:20.783] [RxComputationThreadPool-1] Emitting 1
        //  [59:20.783] [RxComputationThreadPool-1] subscribe 1 : 1
        //  [59:21.788] [RxComputationThreadPool-2] Emitting 2
        //  [59:21.788] [main] Disposing first subscription.
        //  [59:21.788] [RxComputationThreadPool-2] subscribe 1 : 2
        //  [59:21.788] [RxComputationThreadPool-2] subscribe 2 : 2
        //  [59:22.786] [RxComputationThreadPool-3] Emitting 3
        //  [59:22.786] [RxComputationThreadPool-3] subscribe 2 : 3
        //  [59:22.789] [main] Disposing second subscription.
        //  [59:22.790] [main] disposing..
        //  [59:23.296] [main] subscribing..
        //  [59:23.297] [RxComputationThreadPool-2] Emitting 1
        //  [59:23.297] [main] 1
        //  [59:24.300] [RxComputationThreadPool-3] Emitting 2
        //  [59:24.300] [main] 2
        //  [59:25.297] [RxComputationThreadPool-4] Emitting 3
        //  [59:25.297] [main] 3
        //  [59:26.300] [RxComputationThreadPool-1] Emitting 4
        //  [59:26.301] [main] 4
        //  [59:27.301] [RxComputationThreadPool-2] Emitting 5
        //  [59:27.301] [main] 5

        // refCount 를 이용하면 hot / cold 를 섞는 느낌으로 쓸 수 있는 것 같다.
        val connectableStream = generate("1,2,3,4,5", 1000)
                .doOnSubscribe { pp("subscribing..") }
                .doOnDispose { pp("disposing..") }
                .printNext("Emitting ")
                .publish()
                .refCount()

        val disposable1: Disposable = connectableStream.subscribe { pp("subscribe 1 : $it") }
        val disposable2: Disposable = connectableStream.subscribe { pp("subscribe 2 : $it") }

        Thread.sleep(1000)
        pp("Disposing first subscription.")
        disposable1.dispose()

        Thread.sleep(1000)
        pp("Disposing second subscription.")
        disposable2.dispose()

        Thread.sleep(500)
        connectableStream.print()
    }

    @Test
    fun connectableReplay() {

        //  [04:03.137] [main] subscribing to main stream.
        //  [04:03.151] [RxComputationThreadPool-1] subscribe 1 : 1
        //  [04:03.151] [main] Waiting 1.5s and subscribing second stream.
        //  [04:04.152] [RxComputationThreadPool-2] subscribe 1 : 2
        //  [04:04.653] [main] subscribe 2 : 1
        //  [04:04.653] [main] subscribe 2 : 2
        //  [04:04.653] [main] Waiting 1s and blocking subscribing.
        //  [04:05.151] [RxComputationThreadPool-3] subscribe 1 : 3
        //  [04:05.151] [RxComputationThreadPool-3] subscribe 2 : 3
        //  [04:05.656] [main] 1
        //  [04:05.656] [main] 2
        //  [04:05.656] [main] 3
        //  [04:06.152] [RxComputationThreadPool-4] subscribe 1 : 4
        //  [04:06.152] [RxComputationThreadPool-4] subscribe 2 : 4
        //  [04:06.153] [main] 4
        //  [04:07.656] [main] Subscribing to replay stream after completion.
        //  [04:07.657] [main] 1
        //  [04:07.657] [main] 2
        //  [04:07.657] [main] 3
        //  [04:07.657] [main] 4

        val replayStream = generate("1,2,3,4", 1000)
                .doOnSubscribe { pp("subscribing to main stream.") }
                .replay()
                .autoConnect() // refCount() 로 변경해서 테스트 해 보는 것도 의미가 있음. 마지막 subscription 의 behavior 가 달라짐.

        replayStream.subscribe { pp("subscribe 1 : $it") }

        pp("Waiting 1.5s and subscribing second stream.")
        Thread.sleep(1500)
        replayStream.subscribe { pp("subscribe 2 : $it") }

        pp("Waiting 1s and blocking subscribing.")
        Thread.sleep(1000)
        replayStream.print()

        Thread.sleep(1500)
        pp("Subscribing to replay stream after completion.")
        replayStream.print()
    }
}

