import React from 'react'
import { View, Text, StyleSheet } from 'react-native'
import type { NativeStackScreenProps } from '@react-navigation/native-stack'
import type { RootStackParamList } from '../navigation/types'
import { ApexButton } from '../components/ApexButton'
import { Colors } from '../theme/colors'

type Props = NativeStackScreenProps<RootStackParamList, 'Welcome'>

export function WelcomeScreen({ navigation }: Props) {
  return (
    <View style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>Scanify</Text>
        <Text style={styles.tagline}>Mobile check deposit, simplified.</Text>
      </View>
      <View style={styles.buttons}>
        <ApexButton
          title="Deposit a Check"
          onPress={() => navigation.navigate('InvestorStack')}
        />
        <ApexButton
          title="Operator Console"
          variant="secondary"
          onPress={() => navigation.navigate('OperatorStack')}
          style={{ marginTop: 12 }}
        />
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
    justifyContent: 'space-between',
    paddingHorizontal: 24,
    paddingTop: 120,
    paddingBottom: 60,
  },
  content: {
    alignItems: 'center',
  },
  title: {
    fontSize: 48,
    fontWeight: '800',
    color: Colors.accent,
    letterSpacing: -1,
  },
  tagline: {
    fontSize: 18,
    color: Colors.textMuted,
    marginTop: 12,
  },
  buttons: {
    width: '100%',
  },
})
