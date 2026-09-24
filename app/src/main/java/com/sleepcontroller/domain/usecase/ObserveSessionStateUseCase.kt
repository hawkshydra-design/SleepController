package com.sleepcontroller.domain.usecase

import com.sleepcontroller.domain.model.SessionState
import com.sleepcontroller.domain.repository.ISessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the current session state as a reactive flow.
 * Provides a clean domain-level API for ViewModels and UI.
 */
class ObserveSessionStateUseCase @Inject constructor(
    private val sessionRepository: ISessionRepository
) {
    operator fun invoke(): Flow<SessionState> = sessionRepository.sessionState
}
