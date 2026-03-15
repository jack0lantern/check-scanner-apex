import React, { useState } from 'react'
import { View, Text, ScrollView, Alert, StyleSheet } from 'react-native'
import type { NativeStackScreenProps } from '@react-navigation/native-stack'
import type { InvestorStackParamList } from '../../navigation/types'
import { depositApi } from '../../api/apiInstance'
import type { IqaFailureResponse } from '@apex/shared'
import { ApexButton } from '../../components/ApexButton'
import { CheckImageThumbnail } from '../../components/CheckImageThumbnail'
import { FullscreenImageModal } from '../../components/FullscreenImageModal'
import { StepIndicator } from '../../components/StepIndicator'
import { Colors } from '../../theme/colors'

type Props = NativeStackScreenProps<InvestorStackParamList, 'Review'>

export function ReviewScreen({ route, navigation }: Props) {
  const { amount, accountId, frontBase64, backBase64 } = route.params
  const [submitLoading, setSubmitLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryTransferId, setRetryTransferId] = useState<string | undefined>()
  const [fullscreenUri, setFullscreenUri] = useState<string | null>(null)

  async function handleSubmit() {
    setSubmitLoading(true)
    setError(null)

    try {
      const result = await depositApi.submitDeposit({
        frontImage: frontBase64,
        backImage: backBase64,
        amount: parseFloat(amount),
        accountId,
        retryForTransferId: retryTransferId,
      })

      if ('state' in result) {
        navigation.navigate('Status', { transferId: result.transferId })
      }
    } catch (err: unknown) {
      const apiError = err as Error & { status?: number; data?: IqaFailureResponse }
      if (apiError.data?.actionableMessage) {
        setError(apiError.data.actionableMessage)
        if (apiError.data.transferId) {
          setRetryTransferId(apiError.data.transferId)
        }
      } else {
        setError(apiError.message || 'Submission failed')
      }
    } finally {
      setSubmitLoading(false)
    }
  }

  function handleEditDetails() {
    Alert.alert(
      'Going back will require retaking your photos',
      'Continue?',
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Continue', onPress: () => navigation.popToTop() },
      ]
    )
  }

  const frontUri = `data:image/jpeg;base64,${frontBase64}`
  const backUri = `data:image/jpeg;base64,${backBase64}`

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <StepIndicator current={4} total={4} />

      <Text style={styles.header}>Does this look right?</Text>

      <Text style={styles.label}>Front</Text>
      <CheckImageThumbnail uri={frontUri} onPress={() => setFullscreenUri(frontUri)} />

      <Text style={[styles.label, { marginTop: 16 }]}>Back</Text>
      <CheckImageThumbnail uri={backUri} onPress={() => setFullscreenUri(backUri)} />

      <View style={styles.details}>
        <Text style={styles.detailLabel}>Account ID</Text>
        <Text style={styles.detailValue}>{accountId}</Text>
        <Text style={[styles.detailLabel, { marginTop: 12 }]}>Amount</Text>
        <Text style={styles.detailValue}>${parseFloat(amount).toFixed(2)}</Text>
      </View>

      {error && (
        <View style={styles.errorBox}>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      )}

      <View style={styles.buttons}>
        {error && retryTransferId ? (
          <ApexButton
            title="Retake & Retry"
            onPress={() => navigation.popToTop()}
          />
        ) : (
          <ApexButton
            title="Confirm & Submit"
            onPress={handleSubmit}
            loading={submitLoading}
            disabled={submitLoading}
          />
        )}
        <ApexButton
          title="Edit Details"
          variant="secondary"
          onPress={handleEditDetails}
          style={{ marginTop: 12 }}
        />
      </View>

      <FullscreenImageModal uri={fullscreenUri} onClose={() => setFullscreenUri(null)} />
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
  },
  content: {
    paddingHorizontal: 24,
    paddingBottom: 40,
  },
  header: {
    fontSize: 22,
    fontWeight: '700',
    color: Colors.text,
    textAlign: 'center',
    marginBottom: 20,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.textMuted,
    marginBottom: 8,
  },
  details: {
    marginTop: 20,
    backgroundColor: Colors.bgCard,
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  detailLabel: {
    fontSize: 12,
    color: Colors.textMuted,
    fontWeight: '500',
  },
  detailValue: {
    fontSize: 18,
    color: Colors.text,
    fontWeight: '600',
    marginTop: 2,
  },
  errorBox: {
    marginTop: 16,
    padding: 12,
    backgroundColor: Colors.errorBg,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.error,
  },
  errorText: {
    color: Colors.error,
    fontSize: 14,
    textAlign: 'center',
  },
  buttons: {
    marginTop: 24,
  },
})
