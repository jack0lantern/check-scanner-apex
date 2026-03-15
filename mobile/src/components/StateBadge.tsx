import React from 'react'
import { View, Text, StyleSheet } from 'react-native'
import { Colors } from '../theme/colors'

interface StateBadgeProps {
  state: string
}

function getBadgeColors(state: string): { bg: string; text: string } {
  switch (state) {
    case 'COMPLETED':
    case 'APPROVED':
    case 'FUNDS_POSTED':
      return { bg: Colors.successBg, text: Colors.success }
    case 'REJECTED':
    case 'RETURNED':
      return { bg: Colors.errorBg, text: Colors.error }
    case 'ANALYZING':
    case 'VALIDATING':
      return { bg: Colors.warningBg, text: Colors.warning }
    default:
      return { bg: Colors.accentGlow, text: Colors.accent }
  }
}

export function StateBadge({ state }: StateBadgeProps) {
  const colors = getBadgeColors(state)

  return (
    <View style={[styles.badge, { backgroundColor: colors.bg }]}>
      <Text style={[styles.text, { color: colors.text }]}>{state}</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  badge: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    alignSelf: 'flex-start',
  },
  text: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
})
