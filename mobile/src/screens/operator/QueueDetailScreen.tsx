import React, { useEffect, useState } from 'react'
import {
  View,
  Text,
  ScrollView,
  Modal,
  TextInput,
  StyleSheet,
} from 'react-native'
import type { NativeStackScreenProps } from '@react-navigation/native-stack'
import type { OperatorStackParamList } from '../../navigation/types'
import type { OperatorQueueItem } from '@apex/shared'
import { operatorApi } from '../../api/apiInstance'
import { ApexButton } from '../../components/ApexButton'
import { StateBadge } from '../../components/StateBadge'
import { VendorScoreBadge } from '../../components/VendorScoreBadge'
import { CheckImageThumbnail } from '../../components/CheckImageThumbnail'
import { FullscreenImageModal } from '../../components/FullscreenImageModal'
import { Colors } from '../../theme/colors'

type Props = NativeStackScreenProps<OperatorStackParamList, 'QueueDetail'>

export function QueueDetailScreen({ route, navigation }: Props) {
  const { transferId } = route.params
  const [item, setItem] = useState<OperatorQueueItem | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [rejectModalVisible, setRejectModalVisible] = useState(false)
  const [rejectReason, setRejectReason] = useState('')
  const [fullscreenUri, setFullscreenUri] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        const queue = await operatorApi.getOperatorQueue({})
        const found = queue.find((q) => q.transferId === transferId)
        if (found) setItem(found)
      } catch {
        // ignore
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [transferId])

  async function handleApprove() {
    setActionLoading(true)
    try {
      await operatorApi.approveDeposit(transferId)
      navigation.goBack()
    } catch {
      setActionLoading(false)
    }
  }

  async function handleReject() {
    setActionLoading(true)
    try {
      await operatorApi.rejectDeposit(transferId, rejectReason)
      setRejectModalVisible(false)
      navigation.goBack()
    } catch {
      setActionLoading(false)
    }
  }

  if (loading) {
    return (
      <View style={styles.centered}>
        <Text style={styles.loadingText}>Loading...</Text>
      </View>
    )
  }

  if (!item) {
    return (
      <View style={styles.centered}>
        <Text style={styles.loadingText}>Transfer not found</Text>
      </View>
    )
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.headerRow}>
        <StateBadge state={item.state} />
        <VendorScoreBadge score={item.vendorScore} />
      </View>

      <View style={styles.card}>
        <DetailRow label="Transfer ID" value={item.transferId} mono />
        <DetailRow label="Account" value={item.investorAccountId} />
        <DetailRow label="Entered Amount" value={`$${item.enteredAmount.toFixed(2)}`} />
        <DetailRow
          label="OCR Amount"
          value={item.ocrAmount != null ? `$${item.ocrAmount.toFixed(2)}` : 'N/A'}
        />
        <DetailRow label="MICR Data" value={item.micrData ?? 'N/A'} mono />
        <DetailRow
          label="MICR Confidence"
          value={item.micrConfidence != null ? `${item.micrConfidence}%` : 'N/A'}
        />
        <DetailRow label="Submitted" value={new Date(item.submittedAt).toLocaleString()} />
      </View>

      {item.frontImage && (
        <>
          <Text style={styles.imageLabel}>Front</Text>
          <CheckImageThumbnail
            uri={item.frontImage}
            onPress={() => setFullscreenUri(item.frontImage!)}
          />
        </>
      )}

      {item.backImage && (
        <>
          <Text style={[styles.imageLabel, { marginTop: 16 }]}>Back</Text>
          <CheckImageThumbnail
            uri={item.backImage}
            onPress={() => setFullscreenUri(item.backImage!)}
          />
        </>
      )}

      <View style={styles.actions}>
        <ApexButton
          title="Approve"
          onPress={handleApprove}
          loading={actionLoading}
          disabled={actionLoading}
        />
        <ApexButton
          title="Reject"
          variant="secondary"
          onPress={() => setRejectModalVisible(true)}
          disabled={actionLoading}
          style={{ marginTop: 12 }}
        />
      </View>

      <FullscreenImageModal uri={fullscreenUri} onClose={() => setFullscreenUri(null)} />

      <Modal
        visible={rejectModalVisible}
        transparent
        animationType="slide"
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalSheet}>
            <Text style={styles.modalTitle}>Reason for rejection</Text>
            <TextInput
              style={styles.modalInput}
              multiline
              autoFocus
              placeholder="Enter rejection reason..."
              placeholderTextColor={Colors.textMuted}
              value={rejectReason}
              onChangeText={setRejectReason}
            />
            <ApexButton
              title="Submit Rejection"
              onPress={handleReject}
              loading={actionLoading}
              disabled={!rejectReason.trim() || actionLoading}
            />
            <ApexButton
              title="Cancel"
              variant="secondary"
              onPress={() => {
                setRejectModalVisible(false)
                setRejectReason('')
              }}
              style={{ marginTop: 12 }}
            />
          </View>
        </View>
      </Modal>
    </ScrollView>
  )
}

function DetailRow({
  label,
  value,
  mono,
}: {
  label: string
  value: string
  mono?: boolean
}) {
  return (
    <View style={detailStyles.row}>
      <Text style={detailStyles.label}>{label}</Text>
      <Text
        style={[detailStyles.value, mono && detailStyles.mono]}
        numberOfLines={1}
      >
        {value}
      </Text>
    </View>
  )
}

const detailStyles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  label: {
    fontSize: 13,
    color: Colors.textMuted,
    fontWeight: '500',
  },
  value: {
    fontSize: 14,
    color: Colors.text,
    fontWeight: '600',
    maxWidth: '55%',
    textAlign: 'right',
  },
  mono: {
    fontFamily: 'monospace',
    fontSize: 12,
  },
})

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
  },
  content: {
    paddingHorizontal: 24,
    paddingBottom: 40,
  },
  centered: {
    flex: 1,
    backgroundColor: Colors.bg,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    color: Colors.textMuted,
    fontSize: 16,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 16,
    marginBottom: 16,
  },
  card: {
    backgroundColor: Colors.bgCard,
    borderRadius: 12,
    padding: 16,
    borderWidth: 1,
    borderColor: Colors.border,
    marginBottom: 20,
  },
  imageLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.textMuted,
    marginBottom: 8,
  },
  actions: {
    marginTop: 24,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'flex-end',
  },
  modalSheet: {
    backgroundColor: Colors.bgElevated,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
    paddingBottom: 40,
    borderTopWidth: 1,
    borderColor: Colors.border,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: Colors.text,
    marginBottom: 16,
  },
  modalInput: {
    backgroundColor: Colors.bgCard,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 12,
    padding: 16,
    color: Colors.text,
    fontSize: 16,
    minHeight: 100,
    textAlignVertical: 'top',
    marginBottom: 20,
  },
})
