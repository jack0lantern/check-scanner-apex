import React, { useEffect, useRef, useState } from 'react'
import { View, Text, StyleSheet, Platform } from 'react-native'
import type { NativeStackScreenProps } from '@react-navigation/native-stack'
import * as Haptics from 'expo-haptics'
import type { TransferStatusResponse } from '@apex/shared'
import type { InvestorStackParamList } from '../../navigation/types'
import { depositApi } from '../../api/apiInstance'
import { ApexButton } from '../../components/ApexButton'
import { StateBadge } from '../../components/StateBadge'
import { Colors } from '../../theme/colors'

type Props = NativeStackScreenProps<InvestorStackParamList, 'Status'>

const PIPELINE_STATES = [
  'REQUESTED',
  'VALIDATING',
  'ANALYZING',
  'APPROVED',
  'FUNDS_POSTED',
  'COMPLETED',
]

const TERMINAL_STATES = ['COMPLETED', 'REJECTED', 'RETURNED']

export function StatusScreen({ route, navigation }: Props) {
  const { transferId } = route.params
  const [status, setStatus] = useState<TransferStatusResponse | null>(null)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    async function poll() {
      try {
        const data = await depositApi.getDepositStatus(transferId)
        setStatus(data)

        if (TERMINAL_STATES.includes(data.state)) {
          if (intervalRef.current) {
            clearInterval(intervalRef.current)
            intervalRef.current = null
          }
          if (data.state === 'COMPLETED') {
            Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success)
          }
        }
      } catch {
        // Keep polling on error
      }
    }

    poll()
    intervalRef.current = setInterval(poll, 3000)

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [transferId])

  const currentState = status?.state ?? 'REQUESTED'
  const isTerminal = TERMINAL_STATES.includes(currentState)
  const isSuccess = currentState === 'COMPLETED'
  const isFailure = currentState === 'REJECTED' || currentState === 'RETURNED'

  const currentIndex = PIPELINE_STATES.indexOf(currentState)

  return (
    <View style={styles.container}>
      <View style={styles.pipeline}>
        {PIPELINE_STATES.map((state, index) => {
          const isReached = currentIndex >= index
          const isCurrent = currentState === state

          return (
            <View key={state} style={styles.pipelineStep}>
              <View
                style={[
                  styles.dot,
                  isReached && styles.dotReached,
                  isCurrent && !isTerminal && styles.dotCurrent,
                ]}
              >
                {isReached && <Text style={styles.check}>✓</Text>}
              </View>
              <Text
                style={[
                  styles.stateLabel,
                  isReached && styles.stateLabelReached,
                  isCurrent && styles.stateLabelCurrent,
                ]}
              >
                {state}
              </Text>
            </View>
          )
        })}
      </View>

      {currentState === 'ANALYZING' && (
        <Text style={styles.infoMessage}>
          Your check is being reviewed by our team. This may take a moment.
        </Text>
      )}

      {isTerminal && (
        <View style={styles.terminalBox}>
          <StateBadge state={currentState} />
          {isSuccess && (
            <>
              <Text style={[styles.terminalText, { color: Colors.success }]}>
                Deposit Completed
              </Text>
              <Text style={styles.transferId}>Transfer: {transferId}</Text>
            </>
          )}
          {isFailure && (
            <>
              <Text style={[styles.terminalText, { color: Colors.error }]}>
                {currentState === 'REJECTED' ? 'Deposit Rejected' : 'Deposit Returned'}
              </Text>
              {status?.vendorMessage && (
                <Text style={styles.vendorMessage}>{status.vendorMessage}</Text>
              )}
            </>
          )}
        </View>
      )}

      {isTerminal && (
        <ApexButton
          title="Done"
          onPress={() => navigation.popToTop()}
          style={{ marginTop: 24 }}
        />
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
    paddingHorizontal: 24,
    paddingTop: 24,
    paddingBottom: 40,
  },
  pipeline: {
    gap: 4,
  },
  pipelineStep: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 8,
  },
  dot: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: Colors.bgCard,
    borderWidth: 2,
    borderColor: Colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dotReached: {
    backgroundColor: Colors.accent,
    borderColor: Colors.accent,
  },
  dotCurrent: {
    borderColor: Colors.accentHover,
    shadowColor: Colors.accent,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.5,
    shadowRadius: 8,
  },
  check: {
    color: Colors.bg,
    fontSize: 14,
    fontWeight: '700',
  },
  stateLabel: {
    fontSize: 14,
    color: Colors.textMuted,
    fontWeight: '500',
  },
  stateLabelReached: {
    color: Colors.text,
  },
  stateLabelCurrent: {
    fontWeight: '700',
  },
  infoMessage: {
    marginTop: 20,
    padding: 12,
    backgroundColor: Colors.warningBg,
    borderRadius: 8,
    color: Colors.warning,
    fontSize: 14,
    textAlign: 'center',
  },
  terminalBox: {
    marginTop: 24,
    padding: 20,
    backgroundColor: Colors.bgCard,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
    alignItems: 'center',
    gap: 8,
  },
  terminalText: {
    fontSize: 20,
    fontWeight: '700',
  },
  transferId: {
    fontSize: 12,
    color: Colors.textMuted,
    fontFamily: Platform.select({ ios: 'Menlo', default: 'monospace' }),
  },
  vendorMessage: {
    fontSize: 14,
    color: Colors.textMuted,
    textAlign: 'center',
  },
})
