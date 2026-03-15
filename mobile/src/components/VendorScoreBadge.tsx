import React from 'react'
import { View, Text, StyleSheet } from 'react-native'
import { Colors } from '../theme/colors'

interface VendorScoreBadgeProps {
  score: number | null
}

function getScoreColor(score: number): string {
  if (score >= 80) return Colors.success
  if (score >= 50) return Colors.warning
  return Colors.error
}

export function VendorScoreBadge({ score }: VendorScoreBadgeProps) {
  if (score == null) return null

  const color = getScoreColor(score)

  return (
    <View style={styles.container}>
      <Text style={[styles.text, { color }]}>{score}%</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 8,
    backgroundColor: Colors.bgCard,
  },
  text: {
    fontSize: 12,
    fontWeight: '700',
  },
})
