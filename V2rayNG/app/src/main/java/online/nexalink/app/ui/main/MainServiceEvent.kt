package online.nexalink.app.ui.main

sealed class MainServiceEvent {
    data object StateRunning : MainServiceEvent()
    data object StateNotRunning : MainServiceEvent()
    data object StateStartSuccess : MainServiceEvent()
    data class StateStartFailure(val errorMessage: String) : MainServiceEvent()
    data object StateStopSuccess : MainServiceEvent()
    data class MeasureDelaySuccess(val content: String) : MainServiceEvent()
    data object MeasureConfigSuccess : MainServiceEvent()
    data class MeasureConfigNotify(val progress: String) : MainServiceEvent()
    data class MeasureConfigFinish(val finishedCount: String?) : MainServiceEvent()
    // NEXALINK: живая статистика и IP для главного экрана
    data class Stats(val downBps: Long, val upBps: Long, val sessionBytes: Long, val todayBytes: Long, val sessionStartMs: Long) : MainServiceEvent()
    data class IpInfo(val country: String, val ip: String) : MainServiceEvent()
}
