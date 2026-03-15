import React from 'react'
import { View, StyleSheet } from 'react-native'
import { Colors } from '../theme/colors'

interface StepIndicatorProps {
  current: number
  total: number
}

export function StepIndicator({ current, total }: StepIndicatorProps) {
  return (
    <View style={styles.container}>
      {Array.from({ length: total }, (_, i) => (
        <View
          key={i}
          style={[
            styles.dot,
            i < current ? styles.filled : styles.empty,
          ]}
        />
      ))}
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 8,
    marginVertical: 16,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  filled: {
    backgroundColor: Colors.accent,
  },
  empty: {
    backgroundColor: Colors.border,
  },
})
