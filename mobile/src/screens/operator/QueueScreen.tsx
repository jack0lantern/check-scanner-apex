import React, { useCallback, useEffect, useState } from 'react'
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  StyleSheet,
} from 'react-native'
import type { NativeStackScreenProps } from '@react-navigation/native-stack'
import type { OperatorStackParamList } from '../../navigation/types'
import type { OperatorQueueItem } from '@apex/shared'
import { operatorApi } from '../../api/apiInstance'
import { StateBadge } from '../../components/StateBadge'
import { VendorScoreBadge } from '../../components/VendorScoreBadge'
import { Colors } from '../../theme/colors'

type Props = NativeStackScreenProps<OperatorStackParamList, 'Queue'>

export function QueueScreen({ navigation }: Props) {
  const [items, setItems] = useState<OperatorQueueItem[]>([])
  const [refreshing, setRefreshing] = useState(false)

  const loadQueue = useCallback(async () => {
    try {
      const data = await operatorApi.getOperatorQueue({})
      setItems(data)
    } catch {
      // Silently fail on refresh
    }
  }, [])

  useEffect(() => {
    loadQueue()
  }, [loadQueue])

  async function handleRefresh() {
    setRefreshing(true)
    await loadQueue()
    setRefreshing(false)
  }

  function renderItem({ item }: { item: OperatorQueueItem }) {
    return (
      <TouchableOpacity
        style={styles.row}
        activeOpacity={0.7}
        onPress={() => navigation.navigate('QueueDetail', { transferId: item.transferId })}
      >
        <View style={styles.rowTop}>
          <Text style={styles.transferId} numberOfLines={1}>
            {item.transferId.substring(0, 8)}...
          </Text>
          <StateBadge state={item.state} />
        </View>
        <View style={styles.rowBottom}>
          <Text style={styles.amount}>${item.enteredAmount.toFixed(2)}</Text>
          <Text style={styles.account}>{item.investorAccountId}</Text>
          <VendorScoreBadge score={item.vendorScore} />
        </View>
      </TouchableOpacity>
    )
  }

  return (
    <View style={styles.container}>
      <FlatList
        data={items}
        keyExtractor={(item) => item.transferId}
        renderItem={renderItem}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={handleRefresh}
            tintColor={Colors.accent}
          />
        }
        ListEmptyComponent={
          <Text style={styles.emptyText}>No items in queue</Text>
        }
        contentContainerStyle={items.length === 0 ? styles.emptyContainer : undefined}
      />
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
  },
  row: {
    backgroundColor: Colors.bgCard,
    marginHorizontal: 16,
    marginTop: 12,
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  rowTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  rowBottom: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  transferId: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.text,
    fontFamily: 'monospace',
  },
  amount: {
    fontSize: 16,
    fontWeight: '700',
    color: Colors.text,
  },
  account: {
    fontSize: 13,
    color: Colors.textMuted,
    flex: 1,
  },
  emptyText: {
    color: Colors.textMuted,
    fontSize: 16,
    textAlign: 'center',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
})
