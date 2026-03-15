import React from 'react'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import type { OperatorStackParamList } from './types'
import { QueueScreen } from '../screens/operator/QueueScreen'
import { QueueDetailScreen } from '../screens/operator/QueueDetailScreen'
import { Colors } from '../theme/colors'

const Stack = createNativeStackNavigator<OperatorStackParamList>()

export function OperatorNavigator() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: Colors.bgElevated },
        headerTintColor: Colors.text,
        headerTitleStyle: { fontWeight: '600' },
      }}
    >
      <Stack.Screen
        name="Queue"
        component={QueueScreen}
        options={{ title: 'Operator Queue' }}
      />
      <Stack.Screen
        name="QueueDetail"
        component={QueueDetailScreen}
        options={{ title: 'Deposit Details' }}
      />
    </Stack.Navigator>
  )
}
