import React, { useState } from 'react'
import { View, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native'
import type { NativeStackScreenProps } from '@react-navigation/native-stack'
import type { InvestorStackParamList } from '../../navigation/types'
import { ApexButton } from '../../components/ApexButton'
import { ApexTextInput } from '../../components/ApexTextInput'
import { StepIndicator } from '../../components/StepIndicator'
import { Colors } from '../../theme/colors'

type Props = NativeStackScreenProps<InvestorStackParamList, 'Input'>

export function InputScreen({ navigation }: Props) {
  const [accountId, setAccountId] = useState('TEST001')
  const [amount, setAmount] = useState('')

  const canContinue = accountId.trim().length > 0 && parseFloat(amount) > 0

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.content}>
        <StepIndicator current={1} total={4} />
        <ApexTextInput
          label="Account ID"
          value={accountId}
          onChangeText={setAccountId}
          placeholder="e.g. TEST001"
          autoCapitalize="none"
          autoCorrect={false}
        />
        <ApexTextInput
          label="Amount ($)"
          value={amount}
          onChangeText={setAmount}
          placeholder="0.00"
          keyboardType="decimal-pad"
        />
      </View>
      <ApexButton
        title="Continue →"
        onPress={() =>
          navigation.navigate('CameraFront', {
            amount,
            accountId: accountId.trim(),
            side: 'front',
          })
        }
        disabled={!canContinue}
      />
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.bg,
    paddingHorizontal: 24,
    paddingBottom: 40,
    justifyContent: 'space-between',
  },
  content: {
    paddingTop: 8,
  },
})
